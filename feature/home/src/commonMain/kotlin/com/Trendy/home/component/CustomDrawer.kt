package com.Trendy.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Trendy.home.domain.DrawerItem
import com.trendy.shared.BebasNeueFont
import com.trendy.shared.FontSize
import com.trendy.shared.TextPrimary
import com.trendy.shared.TextSecondary
import com.trendy.shared.domain.Customer
import com.trendy.shared.util.RequestState

@Composable
fun CustomDrawer(
    onProfileClick: () -> Unit,
    onContactUsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onAdminPanelClick: () -> Unit,
    customer: RequestState<Customer>,
){
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.6f)
            .padding(
                horizontal = 12.dp
            )
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Trendy",
            textAlign = TextAlign.Center,
            fontFamily = BebasNeueFont(),
            fontSize = FontSize.EXTRA_LARGE,
            color = TextSecondary
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "The hottest trends – all in one place!",
            textAlign = TextAlign.Center,
            fontSize = FontSize.REGULAR,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(50.dp))

        DrawerItem.entries.take(3).forEach { item ->
            DrawerItemCard(
                drawerItem = item,
                onClick = {
                    when (item){
                        DrawerItem.Profile -> onProfileClick()
                        DrawerItem.Contact -> onContactUsClick()
                        DrawerItem.SignOut -> onSignOutClick()
                        else -> {}
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedContent(
            targetState = customer,
        ) { customerState ->
            if(customerState.isSuccess() && customerState.getSuccessData().isAdmin) {
                DrawerItemCard(
                    drawerItem = DrawerItem.Admin,
                    onClick = {
                        onAdminPanelClick()
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}