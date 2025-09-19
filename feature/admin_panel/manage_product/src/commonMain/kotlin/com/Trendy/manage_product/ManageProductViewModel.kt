package com.Trendy.manage_product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Trendy.data.domain.AdminRepository
import com.trendy.shared.domain.Product
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.RequestState
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
data class ManageProductScreenState (
    val id: String = Uuid.random().toHexString(),
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val category: ProductCategory = ProductCategory.Gadgets,
    val color: String = "",
    val weight: Int? = null,
    val price: Double = 0.0,
    val isNew: Boolean = false,
    val isPopular: Boolean = false,
    val isDiscounted: Boolean = false
)

class ManageProductViewModel(
    private val adminRepository: AdminRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {

    private val productId = savedStateHandle.get<String>("id")?: ""

    var screenState by mutableStateOf(ManageProductScreenState())
        private set

    var uploadImageState: RequestState<Unit> by mutableStateOf(RequestState.Idle)
        private set

    val isFormValid: Boolean
        get() = screenState.title.isNotEmpty() &&
                screenState.description.isNotEmpty() &&
                screenState.thumbnail.isNotEmpty() &&
                screenState.price != 0.0


    init {
        productId.takeIf { it.isNotEmpty() }?.let { id ->
            viewModelScope.launch {
                val selectedProduct = adminRepository.readProductById(id)
                if (selectedProduct.isSuccess()) {

                    val product = selectedProduct.getSuccessData()

                    updateId(product.id)
                    updateTitle(product.title)
                    updateCreateAt(product.createdAt)
                    updateDescription(product.description)
                    updateThumbnail(product.thumbnail)
                    updateCategory(ProductCategory.valueOf(product.category))
                    updateWeight(product.weight?: 0)
                    updatePrice(product.price)
                    updateNew(product.isNew)
                    updatePopular(product.isPopular)
                    updateDiscounted(product.isDiscounted)

                    if (product.thumbnail.isNotEmpty()) {
                        updateThumbnailUploaderState(RequestState.Success(Unit))
                    }
                }
            }
        }
    }

    fun updateId(id: String){
        screenState = screenState.copy(id = id)
    }

    fun updateTitle(title: String){
        screenState = screenState.copy(title = title)
    }

    fun updateCreateAt(createdAt: Long){
        screenState = screenState.copy(createdAt = createdAt)
    }

    fun updateDescription(description: String){
        screenState = screenState.copy(description = description)
    }

    fun updateThumbnail(thumbnail: String){
        screenState = screenState.copy(thumbnail = thumbnail)
    }

    fun updateThumbnailUploaderState(value: RequestState<Unit>){
        uploadImageState = value
    }

    fun updateCategory(category: ProductCategory){
        screenState = screenState.copy(category = category)
    }

    fun updateWeight(weight: Int){
        screenState = screenState.copy(weight = weight)
    }

    fun updatePrice(price: Double){
        screenState = screenState.copy(price = price)
    }

    fun updateNew(value: Boolean) {
        screenState = screenState.copy(isNew = value)
    }

    fun updatePopular(value: Boolean) {
        screenState = screenState.copy(isPopular = value)
    }

    fun updateDiscounted(value: Boolean) {
        screenState = screenState.copy(isDiscounted = value)
    }

    fun createNewProduct(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        viewModelScope.launch {
            adminRepository.createNewProduct(
                product = Product(
                    id = screenState.id,
                    title = screenState.title,
                    description = screenState.description,
                    thumbnail = screenState.thumbnail,
                    category = screenState.category.name,
                    weight = screenState.weight,
                    price = screenState.price,
                    isNew = screenState.isNew,
                    isPopular = screenState.isPopular,
                    isDiscounted = screenState.isDiscounted
                ),
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    fun uploadImageToStorage(
        file: File?,
        onSuccess: () -> Unit
    ){

        if (file == null){
            updateThumbnailUploaderState(RequestState.Error("File is null"))
            return
        }

        updateThumbnailUploaderState(RequestState.Loading)

        viewModelScope.launch {
           try {

               val downloadUrl = adminRepository.uploadImageToStorage(file)

               if (downloadUrl.isNullOrEmpty()){
                   throw Exception("Failed to retrieve a download URL after the uploaded")
               }

               productId.takeIf { it.isNotEmpty() }?.let { id ->
                    adminRepository.updateProductThumbnail(
                        productId = id,
                        downloadUrl = downloadUrl,
                        onSuccess = {
                            onSuccess()
                            updateThumbnailUploaderState(RequestState.Success(Unit))
                            updateThumbnail(downloadUrl)
                        },
                        onError = {
                            updateThumbnailUploaderState(RequestState.Error(it))
                        }
                    )
               }?: run {
                   onSuccess()
                   updateThumbnailUploaderState(RequestState.Success(Unit))
                   updateThumbnail(downloadUrl)
               }

           } catch (e: Exception){
               updateThumbnailUploaderState(RequestState.Error("Error while uploading image: ${e.message}"))
           }
        }
    }

    fun updateProduct(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isFormValid){
            viewModelScope.launch {
                adminRepository.updateProduct(
                    product = Product(
                        id = screenState.id,
                        createdAt = screenState.createdAt,
                        title = screenState.title,
                        description = screenState.description,
                        thumbnail = screenState.thumbnail,
                        category = screenState.category.name,
                        weight = screenState.weight,
                        price = screenState.price,
                        isNew = screenState.isNew,
                        isPopular = screenState.isPopular,
                        isDiscounted = screenState.isDiscounted
                    ),
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        }else{
            onError("Please fill in the information ")
        }
    }

    fun deleteImageFromStorage(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        viewModelScope.launch {
            adminRepository.deleteImageFromStorage(
                screenState.thumbnail,
                onSuccess = {
                    productId.takeIf { it.isNotEmpty() }?.let{
                        viewModelScope.launch {
                            adminRepository.updateProductThumbnail(
                                productId = it,
                                downloadUrl = "",
                                onSuccess = {
                                    updateThumbnail("")
                                    updateThumbnailUploaderState(RequestState.Idle)
                                    onSuccess()
                                },
                                onError = {
                                    updateThumbnailUploaderState(RequestState.Error(it))
                                }
                            )
                        }
                    }?: run {
                        updateThumbnail("")
                        updateThumbnailUploaderState(RequestState.Idle)
                        onSuccess()
                    }
                },
                onError = onError
            )
        }
    }

    fun deleteProduct(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        productId.takeIf { it.isNotEmpty() }?.let { id ->
            viewModelScope.launch {
                adminRepository.deleteProduct(
                    productId = id,
                    onSuccess = {
                        deleteImageFromStorage(
                            onSuccess = {},
                            onError = {}
                        )
                        onSuccess()
                    },
                    onError = {
                        onError(it)
                    }
                )
            }
        }
    }
}