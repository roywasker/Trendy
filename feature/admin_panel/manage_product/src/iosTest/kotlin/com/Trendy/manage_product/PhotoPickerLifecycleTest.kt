@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.Trendy.manage_product

import dev.gitlive.firebase.storage.File
import kotlin.native.runtime.GC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIViewController

class PhotoPickerLifecycleTest {
    @Test
    fun unavailablePresenter_returnsNullResetsAndAllowsRetry() {
        val presenter = FakePresenter(shown = false)
        val lifecycle = PhotoPickerLifecycle(presenter)
        var callbackCalls = 0
        var result: File? = null

        lifecycle.open()
        assertFalse(lifecycle.present { callbackCalls++; result = it })

        assertEquals(null, result)
        assertEquals(1, callbackCalls)
        assertFalse(lifecycle.isOpen)
        presenter.shown = true
        lifecycle.open()
        assertTrue(lifecycle.present { result = it })
        assertTrue(lifecycle.isOpen)
    }

    @Test
    fun activePresenter_keepsCallbackUntilItDeliversSelection() {
        val presenter = FakePresenter(shown = true)
        val lifecycle = PhotoPickerLifecycle(presenter)
        var callbackCalls = 0

        lifecycle.open()
        lifecycle.present { callbackCalls++ }
        assertTrue(lifecycle.isOpen)
        presenter.complete(null)

        assertEquals(1, callbackCalls)
        assertFalse(lifecycle.isOpen)
    }

    @Test
    fun uiKitPresenter_keepsDelegateAliveUntilCancelCallback() {
        val controller = RecordingViewController()
        val presenter = UIKitPhotoPickerPresenter { controller }
        var callbackCalls = 0
        var result: File? = null

        assertTrue(presenter.present { file -> callbackCalls++; result = file })
        val picker = assertNotNull(controller.presentedPicker)

        val owner = kotlinx.cinterop.StableRef.create(presenter)
        try {
            GC.collect()
            val delegate = assertNotNull(picker.delegate) as UIImagePickerControllerDelegateProtocol
            delegate.imagePickerControllerDidCancel(picker)
        } finally {
            owner.dispose()
        }

        assertEquals(1, callbackCalls)
        assertNull(result)
    }

    @Test
    fun activeKeyWindowRoot_ignoresBackgroundKeyWindow() {
        val backgroundRoot = UIViewController()
        val foregroundRoot = UIViewController()

        val selected = activeKeyWindowRoot(
            listOf(
                WindowCandidate(isForegroundActive = false, isKeyWindow = true, rootViewController = backgroundRoot),
                WindowCandidate(isForegroundActive = true, isKeyWindow = true, rootViewController = foregroundRoot),
            ),
        )

        assertTrue(selected === foregroundRoot)
    }

    @Test
    fun activeKeyWindowRoot_returnsNullWhenActiveKeyWindowHasNoRoot() {
        val selected = activeKeyWindowRoot(
            listOf(WindowCandidate(isForegroundActive = true, isKeyWindow = true, rootViewController = null)),
        )

        assertNull(selected)
    }

    private class FakePresenter(var shown: Boolean) : PhotoPickerPresenter {
        private var callback: ((File?) -> Unit)? = null

        override fun present(onImageSelect: (File?) -> Unit): Boolean {
            if (!shown) return false
            callback = onImageSelect
            return true
        }

        fun complete(file: File?) {
            callback?.invoke(file)
        }
    }

    private class RecordingViewController : UIViewController(nibName = null, bundle = null) {
        var presentedPicker: UIImagePickerController? = null

        override fun presentViewController(
            viewControllerToPresent: UIViewController,
            animated: Boolean,
            completion: (() -> Unit)?,
        ) {
            presentedPicker = viewControllerToPresent as? UIImagePickerController
            completion?.invoke()
        }
    }
}
