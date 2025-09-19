package com.Trendy.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.Trendy.home.domain.DrawerItem
import com.trendy.shared.FontSize
import com.trendy.shared.IconPrimary
import com.trendy.shared.TextPrimary
import org.jetbrains.compose.resources.painterResource

@Composable
fun DrawerItemCard(
    drawerItem: DrawerItem,
    onClick: () -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(size = 99.dp))
            .clickable { onClick() }
            .padding(
                vertical = 12.dp,
                horizontal = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(drawerItem.icon),
            contentDescription = drawerItem.title,
            tint = IconPrimary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = drawerItem.title,
            color = TextPrimary,
            fontSize = FontSize.EXTRA_REGULAR,
        )
    }
}