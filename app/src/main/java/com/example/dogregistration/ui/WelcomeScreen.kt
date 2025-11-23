package com.example.dogregistration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dogregistration.R

@Composable
fun WelcomeScreen(
    onRegisterClick: () -> Unit,
    onScanClick: () -> Unit,
    onViewRegistered: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dark background with circuit board pattern effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
        ) {
            // Circuit board pattern overlay (simulated with gradients)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E).copy(alpha = 0.6f),
                                Color(0xFF0A0A0A)
                            ),
                            radius = 1200f
                        )
                    )
            )
            
            // Additional subtle pattern layers
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00BCD4).copy(alpha = 0.03f),
                                Color.Transparent,
                                Color(0xFF4CAF50).copy(alpha = 0.03f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
            )
        }
        
        // Content on top of background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // CANID Title with gradient styling
            // CAN part: blue to teal, ID part: teal to green
            val canidText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2196F3), // Blue
                                Color(0xFF00BCD4)  // Teal
                            )
                        )
                    )
                ) {
                    append("CAN")
                }
                withStyle(
                    style = SpanStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00BCD4), // Teal
                                Color(0xFF4CAF50)  // Green
                            )
                        )
                    )
                ) {
                    append("ID")
                }
            }
            
            Text(
                text = canidText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 72.sp,
                    letterSpacing = 6.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tagline
            Text(
                text = "Smart Registration & Biometric Identification for Dogs.\nSecure Registry, Instant Verification.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Buttons with glowing, translucent appearance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Register Dog Button - Purple with nose print icon
                GlowingButton(
                    onClick = onRegisterClick,
                    text = "Register Dog",
                    icon = Icons.Default.Pets, // Using Pets icon as nose print alternative
                    color = Color(0xFF9C27B0), // Purple
                    modifier = Modifier.fillMaxWidth()
                )

                // Identify Dog Button - Teal/Cyan with magnifying glass icon
                GlowingButton(
                    onClick = onScanClick,
                    text = "Identify Dog",
                    icon = Icons.Default.Search,
                    color = Color(0xFF00BCD4), // Teal/Cyan
                    modifier = Modifier.fillMaxWidth()
                )

                // View Registered Dogs Button - Pink/Magenta with paw print icon
                GlowingButton(
                    onClick = onViewRegistered,
                    text = "View Registered Dogs",
                    icon = Icons.Default.Pets, // Using Pets icon as paw print
                    color = Color(0xFFE91E63), // Pink/Magenta
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Powered by Nose-Print Tech.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun GlowingButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = color.copy(alpha = 0.5f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.3f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        elevation = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
        }
    }
}
