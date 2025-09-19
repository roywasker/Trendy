package com.trendy.shared.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trendy.shared.Alpha
import com.trendy.shared.ButtonDisabled
import com.trendy.shared.ButtonPrimary
import com.trendy.shared.ButtonSecondary
import com.trendy.shared.FontSize
import com.trendy.shared.IconPrimary
import com.trendy.shared.Resources
import com.trendy.shared.TextPrimary
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enable: Boolean = true,
    secondary: Boolean = false,
    icon: DrawableResource? = null
){
    Button(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        enabled = enable,
        shape = RoundedCornerShape(size = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) ButtonSecondary else ButtonPrimary,
            contentColor = TextPrimary,
            disabledContainerColor = ButtonDisabled,
            disabledContentColor = TextPrimary.copy(alpha = Alpha.DISABLED)
        ),
        contentPadding = PaddingValues(all = 20.dp)
    ){

        if (icon != null){
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(icon),
                contentDescription = "Button icon",
                tint = if (icon == Resources.Images.PaypalLogo) Color.Unspecified
                else IconPrimary

            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = FontSize.REGULAR,
            fontWeight = FontWeight.Medium
        )
    }
}