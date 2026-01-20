# Travelog

旅行・地域写真を管理するマルチプラットフォームアプリ。

## 技術スタック

- **Compose Multiplatform (CMP) / Kotlin Multiplatform (KMP)**
- **Supabase** (PostgreSQL + RLS + PostGIS)
- **対応プラットフォーム**: Android / iOS / JVM

## コマンド

```bash
# Android アプリのビルド・実行
./gradlew :composeApp:assembleDebug
# IDE の Run Configuration「composeApp」を使用

# iOS アプリ
# IDE の Run Configuration「Travelog」を使用

# Detekt（Lint）
make detekt
# または
./gradlew detekt --auto-correct --continue

# Backend (Ktor) 実行
./gradlew :backend:run
```

## アーキテクチャ

### モジュール構成

```
composeApp/          # アプリエントリポイント、DI設定、Supabase初期化
backend/             # Ktor サーバー（API）
core/
  ├── common/        # 共通ユーティリティ、Koin BOM
  ├── model/         # ドメインモデル、DTO
  ├── datasource/    # API クライアント、マッパー
  ├── repository/    # リポジトリ実装
  ├── usecase/       # ユースケース（未使用）
  ├── ui/            # 共通 UI コンポーネント、Destination、ScreenState
  └── resource/      # リソース
feature/
  ├── home/          # ホーム画面（Maps / Photos タブ）
  ├── setting/       # 設定画面
  └── login/         # ログイン画面
```

### データフロー

```
Screen (Composable) → ViewModel → Repository → API (Supabase) → DTO ↔ Mapper ↔ Domain Model
```

- **DTO**: ネットワーク/DB 層のデータ構造（GeoJSON文字列など）
- **Domain Model**: アプリ内で使用するパース済みデータ（座標、ポリゴンなど）
- **Mapper**: DTO ↔ Domain 変換（`core/datasource/helper/`）

### DI (Koin)

各モジュールに `di/` ディレクトリがあり、`*Module.kt` を定義。`composeApp/di/Koin.kt` で全モジュールを集約。

```kotlin
// 例: feature/home/di/HomeModule.kt
val homeModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::HomePhotosViewModel)
}
```

### ナビゲーション

- `Destination` sealed interface（`core/ui/screen/Destination.kt`）で画面定義
- `LocalNavBackStack` で画面遷移を管理

### 画面構成

各 feature モジュールは以下の構造:

- `*Screen.kt`: Composable 関数（`internal fun`）
- `*ViewModel.kt`: 画面ロジック

## build-logic

カスタム Gradle プラグイン（`build-logic/`）:

- `matsumo.primitive.kmp.common`: KMP 共通設定
- `matsumo.primitive.kmp.android`: Android ライブラリ設定
- `matsumo.primitive.kmp.ios`: iOS 設定
- `matsumo.primitive.kmp.compose`: Compose Multiplatform 設定
- `matsumo.primitive.android.application`: Android アプリ設定
- `matsumo.primitive.backend`: Ktor バックエンド設定
- `matsumo.primitive.detekt`: Detekt 設定

## コーディング規約

### Compose UI

- **Modifier**: 呼び出し時は先頭引数、定義時はデフォルト引数の先頭に配置
- **DI**: Composable 内で直接 Repository を inject しない。ViewModel 経由で使用
- Composable 関数は `@Composable` アノテーション付きなら PascalCase 許可（detekt 設定済み）

### 環境変数

`local.properties` または環境変数で設定:

- `SUPABASE_URL`, `SUPABASE_KEY`
- `GOOGLE_CLIENT_ID`
- `ADMOB_*` 系（広告ID）

## 主要ライブラリ

| ライブラリ                 | バージョン       |
|-----------------------|-------------|
| Kotlin                | 2.3.0       |
| Compose Multiplatform | 1.10.0-rc02 |
| Supabase              | 3.2.5       |
| Koin                  | 4.1.1       |
| Ktor                  | 3.3.3       |
| Coil3                 | 3.3.0       |

## ドキュメント

- [データベース設計](docs/DATABASE.md)
