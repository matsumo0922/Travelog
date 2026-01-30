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
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.db.ImageComment
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.CreateImageCommentUseCase
import me.matsumo.travelog.core.usecase.GetPhotoDetailUseCase
import me.matsumo.travelog.core.usecase.UpdateImageCommentsUseCase

class PhotoDetailViewModel(
    private val imageId: String,
    private val initialImageUrl: String?,
    private val imageRepository: ImageRepository,
    private val getPhotoDetailUseCase: GetPhotoDetailUseCase,
    private val createImageCommentUseCase: CreateImageCommentUseCase,
    private val updateImageCommentsUseCase: UpdateImageCommentsUseCase,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<PhotoDetailUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<PhotoDetailUiState>> = _screenState.asStateFlow()

    private val _dialogState = MutableStateFlow<PhotoDetailDialogState>(PhotoDetailDialogState.None)
    val dialogState: StateFlow<PhotoDetailDialogState> = _dialogState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private val _uiEvent = MutableSharedFlow<PhotoDetailUiEvent>()
    val uiEvent: SharedFlow<PhotoDetailUiEvent> = _uiEvent.asSharedFlow()

    private val pendingEdits = mutableMapOf<String, String>()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val detail = getPhotoDetailUseCase(imageId, initialImageUrl)

                PhotoDetailUiState(
                    imageId = detail.imageId,
                    imageUrl = detail.imageUrl,
                    image = detail.image,
                    comments = detail.comments.toImmutableList(),
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
            viewModelScope.launch {
                _isSaving.value = true
                when (val result = createImageCommentUseCase(currentState.imageId, newBody)) {
                    is CreateImageCommentUseCase.Result.Success -> {
                        _screenState.update {
                            val state = (it as? ScreenState.Idle)?.data ?: return@update it
                            ScreenState.Idle(state.copy(comments = result.comments.toImmutableList()))
                        }
                    }

                    is CreateImageCommentUseCase.Result.InvalidBody,
                    is CreateImageCommentUseCase.Result.UserNotLoggedIn,
                    is CreateImageCommentUseCase.Result.Failed,
                    -> {
                        _uiEvent.emit(PhotoDetailUiEvent.CommentSaveFailed)
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
        _screenState.update { ScreenState.Idle(currentState.copy(comments = updatedComments.toImmutableList())) }
        _dialogState.value = PhotoDetailDialogState.None
    }

    fun savePendingEdits() {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        val commentsById = currentState.comments.associateBy { it.id }

        viewModelScope.launch {
            if (pendingEdits.isEmpty()) {
                _navigateBack.emit(Unit)
                return@launch
            }

            _isSaving.value = true

            val updates = pendingEdits.mapNotNull { (id, body) -> commentsById[id]?.copy(body = body) }

            when (val result = updateImageCommentsUseCase(updates)) {
                is UpdateImageCommentsUseCase.Result.Success -> {
                    pendingEdits.clear()
                }

                is UpdateImageCommentsUseCase.Result.Failed -> {
                    _uiEvent.emit(PhotoDetailUiEvent.CommentUpdateFailed)
                }

                else -> Unit
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
}

@Stable
data class PhotoDetailUiState(
    val imageId: String,
    val imageUrl: String?,
    val image: Image?,
    val comments: ImmutableList<ImageComment>,
)

@Stable
sealed interface PhotoDetailDialogState {
    data object None : PhotoDetailDialogState
    data class CommentEdit(val comment: ImageComment?) : PhotoDetailDialogState
}

sealed interface PhotoDetailUiEvent {
    data object CommentSaveFailed : PhotoDetailUiEvent
    data object CommentUpdateFailed : PhotoDetailUiEvent
}
