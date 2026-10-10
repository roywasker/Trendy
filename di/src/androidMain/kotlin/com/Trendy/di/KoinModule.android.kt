package com.Trendy.di

import com.Trendy.manage_product.PhotoPicker
import com.Trendy.manage_product.ImagePicker
import org.koin.dsl.module

actual val targetModule = module {
    single<ImagePicker>{ PhotoPicker() }
}
