package com.Trendy.di

import com.Trendy.manage_product.PhotoPicker
import org.koin.dsl.module

actual val targetModule = module {
    single<PhotoPicker>{ PhotoPicker() }
}