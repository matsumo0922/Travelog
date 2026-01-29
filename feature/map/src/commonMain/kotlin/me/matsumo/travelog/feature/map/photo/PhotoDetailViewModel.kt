package me.matsumo.travelog.feature.map.photo

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.db.ImageComment
import me.matsumo.travelog.core.repository.ImageCommentRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.repository.StorageRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class PhotoDetailViewModel(
    private val imageId: String,
    private val initialImageUrl: String?,
    private val imageRepository: ImageRepository,
    private val imageCommentRepository: ImageCommentRepository,
    private val sessionRepository: SessionRepository,
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<PhotoDetailUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<PhotoDetailUiState>> = _screenState.asStateFlow()

    private val _dialogState = MutableStateFlow<PhotoDetailDialogState>(PhotoDetailDialogState.None)
    val dialogState: StateFlow<PhotoDetailDialogState> = _dialogState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _hasPendingEdits = MutableStateFlow(false)
    val hasPendingEdits: StateFlow<Boolean> = _hasPendingEdits.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private val pendingEdits = mutableMapOf<String, String>()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val image = imageRepository.getImage(imageId)
                val imageUrl = image?.let { resolveImageUrl(it) } ?: initialImageUrl
                val comments = if (imageId.isNotBlank()) {
                    imageCommentRepository.getImageCommentsByImageId(imageId)
                } else {
                    emptyList()
                }

                PhotoDetailUiState(
                    imageId = imageId,
                    imageUrl = imageUrl,
                    image = image,
                    comments = comments.toImmutableList(),
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    fun showCommentEditDialog(comment: ImageComment?) {
        _dialogState.value = PhotoDetailDialogState.CommentEdit(comment)
    }

    fun dismissDialog() {
        _dialogState.value = PhotoDetailDialogState.None
    }

    fun upsertComment(comment: ImageComment?, newBody: String) {
        if (newBody.isBlank()) {
            _dialogState.value = PhotoDetailDialogState.None
            return
        }

        if (comment == null) {
            val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
            val userId = sessionRepository.getCurrentUserInfo()?.id
            if (userId == null) {
                _dialogState.value = PhotoDetailDialogState.None
                return
            }
            viewModelScope.launch {
                _isSaving.value = true
                val result = suspendRunCatching {
                    val newComment = ImageComment(
                        imageId = currentState.imageId,
                        authorUserId = userId,
                        body = newBody,
                    )
                    imageCommentRepository.createImageComment(newComment)
                    imageCommentRepository.getImageCommentsByImageId(currentState.imageId)
                }

                result.onSuccess { latestComments ->
                    _screenState.update {
                        val state = (it as? ScreenState.Idle)?.data ?: return@update it
                        ScreenState.Idle(state.copy(comments = latestComments.toImmutableList()))
                    }
                }

                _isSaving.value = false
                _dialogState.value = PhotoDetailDialogState.None
            }
            return
        }

        val commentId = comment.id ?: return
        if (newBody == comment.body) {
            _dialogState.value = PhotoDetailDialogState.None
            return
        }

        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        val updatedComments = currentState.comments.map { item ->
            if (item.id == commentId) item.copy(body = newBody) else item
        }

        pendingEdits[commentId] = newBody
        _hasPendingEdits.value = pendingEdits.isNotEmpty()
        _screenState.update { ScreenState.Idle(currentState.copy(comments = updatedComments.toImmutableList())) }
        _dialogState.value = PhotoDetailDialogState.None
    }

    fun savePendingEdits() {
        if (pendingEdits.isEmpty()) return
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        val commentsById = currentState.comments.associateBy { it.id }

        viewModelScope.launch {
            _isSaving.value = true
            val result = suspendRunCatching {
                pendingEdits.forEach { (id, body) ->
                    val target = commentsById[id] ?: return@forEach
                    imageCommentRepository.updateImageComment(target.copy(body = body))
                }
            }

            if (result.isSuccess) {
                pendingEdits.clear()
                _hasPendingEdits.value = false
            }

            _isSaving.value = false
        }
    }

    fun deleteImage() {
        if (imageId.isBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            val result = suspendRunCatching {
                imageRepository.deleteImage(imageId)
            }

            if (result.isSuccess) {
                _navigateBack.emit(Unit)
            } else {
                _isSaving.value = false
            }
        }
    }

    private suspend fun resolveImageUrl(image: Image): String? {
        val bucketName = image.bucketName ?: return null

        return when (bucketName) {
            StorageApi.BUCKET_MAP_REGION_IMAGES -> {
                storageRepository.getSignedUrl(bucketName, image.storageKey)
            }

            else -> {
                storageRepository.getMapIconPublicUrl(image.storageKey)
            }
        }
    }
}

@Stable
data class PhotoDetailUiState(
    val imageId: String,
    val imageUrl: String?,
    val image: Image?,
    val comments: ImmutableList<ImageComment>,
)

sealed interface PhotoDetailDialogState {
    data object None : PhotoDetailDialogState

    data class CommentEdit(val comment: ImageComment?) : PhotoDetailDialogState
}
