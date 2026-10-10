package com.Trendy.manage_product

import androidx.compose.runtime.Composable
import dev.gitlive.firebase.storage.File

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
interface ImagePicker {
    @Composable
    fun InitializePhotoPicker(onImageSelect: (File?) -> Unit)
    fun open()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PhotoPicker() : ImagePicker {

    @Composable
    override fun InitializePhotoPicker(onImageSelect: (File?) -> Unit )

    override fun open()
}
