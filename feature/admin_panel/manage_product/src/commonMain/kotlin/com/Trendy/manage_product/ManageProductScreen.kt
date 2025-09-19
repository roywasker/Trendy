package com.Trendy.manage_product

import ContentWithMessageBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.trendy.shared.BebasNeueFont
import com.trendy.shared.BorderIdle
import com.trendy.shared.ButtonPrimary
import com.trendy.shared.FontSize
import com.trendy.shared.IconPrimary
import com.trendy.shared.Resources
import com.trendy.shared.Surface
import com.trendy.shared.SurfaceDarker
import com.trendy.shared.SurfaceLighter
import com.trendy.shared.SurfaceSecondary
import com.trendy.shared.TextPrimary
import com.trendy.shared.TextSecondary
import com.trendy.shared.component.AlertTextField
import com.trendy.shared.component.CustomTextField
import com.trendy.shared.component.ErrorCard
import com.trendy.shared.component.LoadingCard
import com.trendy.shared.component.PrimaryButton
import com.trendy.shared.component.dialog.CategoriesDialog
import com.trendy.shared.domain.ProductCategory
import com.trendy.shared.util.DisplayResult
import com.trendy.shared.util.RequestState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductScreen(
    id: String?,
    navigateBack: () -> Unit
){

    val viewModel = koinViewModel<ManageProductViewModel>()
    val messageBarState = rememberMessageBarState()
    val isFormValid = viewModel.isFormValid
    val uploadImageState = viewModel.uploadImageState
    var showCategoryDialog by remember { mutableStateOf(false) }
    val screenState = viewModel.screenState
    val photoPicker = koinInject<PhotoPicker>()
    var dropdownMenuOpened by remember { mutableStateOf(false) }

    photoPicker.InitializePhotoPicker(
        onImageSelect = { file ->
            viewModel.uploadImageToStorage(
                file = file,
                onSuccess = { messageBarState.addSuccess("Image uploaded successfully")}
            )
        }
    )

    AnimatedVisibility(
        visible = showCategoryDialog
    ){
        CategoriesDialog(
            category = screenState.category,
            onDismiss = { showCategoryDialog = false },
            onConfirmClick = { selectedCategory ->
                viewModel.updateCategory(selectedCategory)
                showCategoryDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (id == null) "New Product" else "Edit Product",
                        fontFamily = BebasNeueFont(),
                        fontSize = FontSize.LARGE,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(
                            painter = painterResource(Resources.Icons.BackArrow),
                            contentDescription = "BackArrow icon",
                            tint = IconPrimary
                        )
                    }
                },
                actions = {
                    id.takeIf { it != null }?.let {
                        Box{
                            IconButton(onClick = { dropdownMenuOpened= true }) {
                                Icon(
                                    painter = painterResource(Resources.Icons.VerticalMenu),
                                    contentDescription = "VerticalMenu icon",
                                    tint = IconPrimary
                                )
                            }
                            DropdownMenu(
                                containerColor = Surface,
                                expanded = dropdownMenuOpened,
                                onDismissRequest = { dropdownMenuOpened = false }
                            ){
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            modifier = Modifier.size(14.dp),
                                            painter = painterResource(Resources.Icons.Delete),
                                            contentDescription = "Delete icon",
                                            tint = IconPrimary
                                        )
                                    },
                                    text = { Text(text = "Delete", color = TextPrimary) },
                                    onClick = {
                                        dropdownMenuOpened = false
                                        viewModel.deleteProduct(
                                            onSuccess = navigateBack,
                                            onError = { message ->
                                                messageBarState.addError(message)
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Surface,
                    scrolledContainerColor = Surface,
                    navigationIconContentColor = IconPrimary,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = IconPrimary
                )
            )
        }
    ){ padding ->

        ContentWithMessageBar(
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                ),
            contentBackgroundColor = Surface,
            messageBarState = messageBarState,
            errorMaxLines = 2
        ){
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp
                    )
                    .padding(
                        top = 12.dp,
                        bottom = 24.dp
                    )
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(size = 12.dp))
                            .border(
                                width = 1.dp,
                                color = BorderIdle,
                                shape = RoundedCornerShape(size = 12.dp)
                            )
                            .background(SurfaceLighter)
                            .clickable(
                                enabled = uploadImageState.isIdle(),
                            ){
                                photoPicker.open()
                            },
                        contentAlignment = Alignment.Center
                    ){

                        uploadImageState.DisplayResult(
                            onIdle = {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(Resources.Icons.Plus),
                                    contentDescription = "Add icon",
                                    tint = IconPrimary
                                )
                            },
                            onLoading = {
                                LoadingCard(
                                    modifier = Modifier
                                        .fillMaxSize()
                                )
                            },
                            onSuccess ={
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    AsyncImage(
                                        modifier = Modifier.fillMaxSize(),
                                        model = ImageRequest.Builder(
                                            LocalPlatformContext.current
                                        ).data(screenState.thumbnail)
                                            .crossfade(enable = true)
                                            .build(),
                                        contentDescription = "Product thumbnail image",
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(size = 6.dp))
                                            .padding(
                                                top = 12.dp,
                                                end = 12.dp
                                            )
                                            .background(ButtonPrimary)
                                            .clickable {
                                                viewModel.deleteImageFromStorage(
                                                    onSuccess = { messageBarState.addSuccess("Thumbnail removed successfully.") },
                                                    onError = { message ->
                                                        messageBarState.addError(
                                                            message
                                                        )
                                                    }
                                                )
                                            }
                                            .padding(all = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(14.dp),
                                            painter = painterResource(Resources.Icons.Delete),
                                            contentDescription = "Delete icon"
                                        )
                                    }
                                }
                            },
                            onError ={ message ->

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ErrorCard(
                                        message = message,
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    TextButton(
                                        onClick = {
                                            viewModel.updateThumbnailUploaderState(RequestState.Idle)
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            containerColor = Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            text = "Try Again",
                                            fontSize = FontSize.SMALL,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        )

                    }

                    CustomTextField(
                        value = screenState.title,
                        onValueChange = viewModel::updateTitle,
                        placeholder = "Title"
                    )

                    CustomTextField(
                        modifier = Modifier
                            .height(168.dp),
                        value = screenState.description,
                        onValueChange = viewModel::updateDescription,
                        placeholder = "Description",
                        expanded = true
                    )

                    AlertTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = screenState.category.title,
                        onClick = { showCategoryDialog = true }
                    )

                    AnimatedVisibility(
                        visible = screenState.category != ProductCategory.DIY
                    ) {
                        CustomTextField(
                            value = "${screenState.weight ?: ""}",
                            onValueChange = {
                                val weight = it.toIntOrNull() ?: 0
                                viewModel.updateWeight(weight)
                            },
                            placeholder = "Weight",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )
                    }

                    CustomTextField(
                        value = "${screenState.price}",
                        onValueChange = {
                            val price = it.toDoubleOrNull()?: 0.0
                            viewModel.updatePrice(price)
                        },
                        placeholder = "Price",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 12.dp),
                                text = "New",
                                fontSize = FontSize.REGULAR,
                                color = TextPrimary
                            )
                            Switch(
                                checked = screenState.isNew,
                                onCheckedChange = viewModel::updateNew,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = SurfaceSecondary,
                                    uncheckedTrackColor = SurfaceDarker,
                                    checkedThumbColor = Surface,
                                    uncheckedThumbColor = Surface,
                                    checkedBorderColor = SurfaceSecondary,
                                    uncheckedBorderColor = SurfaceDarker
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 12.dp),
                                text = "Popular",
                                fontSize = FontSize.REGULAR,
                                color = TextPrimary
                            )
                            Switch(
                                checked = screenState.isPopular,
                                onCheckedChange = viewModel::updatePopular,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = SurfaceSecondary,
                                    uncheckedTrackColor = SurfaceDarker,
                                    checkedThumbColor = Surface,
                                    uncheckedThumbColor = Surface,
                                    checkedBorderColor = SurfaceSecondary,
                                    uncheckedBorderColor = SurfaceDarker
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 12.dp),
                                text = "Discounted",
                                fontSize = FontSize.REGULAR,
                                color = TextPrimary
                            )
                            Switch(
                                checked = screenState.isDiscounted,
                                onCheckedChange = viewModel::updateDiscounted,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = SurfaceSecondary,
                                    uncheckedTrackColor = SurfaceDarker,
                                    checkedThumbColor = Surface,
                                    uncheckedThumbColor = Surface,
                                    checkedBorderColor = SurfaceSecondary,
                                    uncheckedBorderColor = SurfaceDarker
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                PrimaryButton(
                    text = if (id == null) "Add new product" else "Update",
                    icon = if (id == null) Resources.Icons.Plus else Resources.Icons.Checkmark,
                    enable = isFormValid,
                    onClick = {

                        if(id != null){
                            viewModel.updateProduct(
                                onSuccess = {
                                    messageBarState.addSuccess("Product updated successfully")
                                },
                                onError = { message ->
                                    messageBarState.addError(message)
                                }
                            )
                        }else {
                            viewModel.createNewProduct(
                                onSuccess = {
                                    messageBarState.addSuccess("Product added successfully")
                                },
                                onError = { message ->
                                    messageBarState.addError(message)
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}