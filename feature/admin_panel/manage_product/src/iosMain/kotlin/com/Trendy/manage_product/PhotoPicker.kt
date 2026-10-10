package com.Trendy.manage_product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.gitlive.firebase.storage.File
import platform.Foundation.writeToFile
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerImageURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindow
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.darwin.NSObject

/** Injectable inside the iOS module so picker lifecycle can be tested without presenting UIKit UI. */
internal interface PhotoPickerPresenter {
    fun present(onImageSelect: (File?) -> Unit): Boolean
}

internal class PhotoPickerLifecycle(private val presenter: PhotoPickerPresenter) {
    var isOpen: Boolean = false
        private set

    fun open() {
        isOpen = true
    }

    fun present(onImageSelect: (File?) -> Unit): Boolean {
        if (!isOpen) return false
        val shown = presenter.present { file ->
            isOpen = false
            onImageSelect(file)
        }
        if (!shown) {
            isOpen = false
            onImageSelect(null)
        }
        return shown
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PhotoPicker actual constructor() : ImagePicker {
    private var openPhotoPicker = mutableStateOf(false)
    private var lifecycle = PhotoPickerLifecycle(UIKitPhotoPickerPresenter())

    internal constructor(presenter: PhotoPickerPresenter) : this() {
        lifecycle = PhotoPickerLifecycle(presenter)
    }

    actual override fun open() {
        lifecycle.open()
        openPhotoPicker.value = true
    }

    @Composable
    actual override fun InitializePhotoPicker(onImageSelect: (File?) -> Unit) {
        val openPhotoPickerState by remember { openPhotoPicker }
        LaunchedEffect(openPhotoPickerState) {
            if (openPhotoPickerState) {
                lifecycle.present { file ->
                    onImageSelect(file)
                    openPhotoPicker.value = false
                }
            }
        }
    }
}

internal data class WindowCandidate(
    val isForegroundActive: Boolean,
    val isKeyWindow: Boolean,
    val rootViewController: UIViewController?,
)

/** The selection rule is kept separate so iOS tests can cover the same scene policy. */
internal fun activeKeyWindowRoot(candidates: Iterable<WindowCandidate>): UIViewController? =
    candidates.firstOrNull { it.isForegroundActive && it.isKeyWindow }?.rootViewController

internal class UIKitPhotoPickerPresenter(
    private val currentController: () -> UIViewController? = ::currentViewController,
) : PhotoPickerPresenter {
    private var pickerDelegate: PickerDelegate? = null

    override fun present(onImageSelect: (File?) -> Unit): Boolean {
        val viewController = currentController() ?: return false
        val delegate = PickerDelegate { file ->
            pickerDelegate = null
            onImageSelect(file)
        }
        pickerDelegate = delegate
        val picker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            mediaTypes = listOf("public.image", "public.heif")
            this.delegate = delegate
        }
        viewController.presentViewController(picker, animated = true, completion = null)
        return true
    }

}

private fun currentViewController(): UIViewController? {
    val root = activeKeyWindowRoot(
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene ->
                scene.windows.filterIsInstance<UIWindow>().map { window ->
                    WindowCandidate(
                        isForegroundActive = scene.activationState == UISceneActivationStateForegroundActive,
                        isKeyWindow = window.isKeyWindow(),
                        rootViewController = window.rootViewController,
                    )
                }
            },
    )
    return topViewController(root)
}

private fun topViewController(viewController: UIViewController?): UIViewController? = when (viewController) {
    is UINavigationController -> topViewController(viewController.visibleViewController)
    is UITabBarController -> topViewController(viewController.selectedViewController)
    is UIViewController -> viewController.presentedViewController?.let(::topViewController) ?: viewController
    else -> null
}

private class PickerDelegate(private val callback: (File?) -> Unit) : NSObject(),
    UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        callback(fileFrom(didFinishPickingMediaWithInfo))
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        callback(null)
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    private fun fileFrom(info: Map<Any?, *>): File? {
        val sourceUrl = info[UIImagePickerControllerImageURL] as? NSURL
        if (sourceUrl != null) return File(sourceUrl)

        val image = info[UIImagePickerControllerOriginalImage] as? UIImage ?: return null
        val data: NSData = UIImageJPEGRepresentation(image, 0.9) ?: return null
        val path = "${NSTemporaryDirectory()}${NSUUID().UUIDString}.jpg"
        return if (data.writeToFile(path, atomically = true)) File(NSURL.fileURLWithPath(path)) else null
    }
}
