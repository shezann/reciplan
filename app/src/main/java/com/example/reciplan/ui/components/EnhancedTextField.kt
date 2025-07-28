package com.example.reciplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reciplan.ui.theme.*

/**
 * Enhanced text field component with modern underlined design and smooth focus states
 * 
 * Features:
 * - Underlined input style with 8dp corner radius (Subtask 71)
 * - Smooth focus state transitions (Subtask 72)
 * - Basil-colored placeholder text at 30% alpha (Subtask 73)
 * 
 * @param value Current text field value
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for styling
 * @param enabled Whether the text field is enabled
 * @param readOnly Whether the text field is read-only
 * @param label Optional label text
 * @param placeholder Optional placeholder text
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param supportingText Optional supporting/helper text
 * @param isError Whether the field is in error state
 * @param keyboardOptions Keyboard configuration
 * @param keyboardActions Keyboard action callbacks
 * @param singleLine Whether the field is single line
 * @param maxLines Maximum number of lines
 * @param minLines Minimum number of lines
 * @param visualTransformation Visual transformation for input
 * @param interactionSource Interaction source for touch handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Subtask 72: Focus State Transitions
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary // Tomato color
            !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "border_color_animation"
    )
    
    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "label_color_animation"
    )
    
    // Subtask 73: Placeholder Styling with Basil color
    val placeholderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) // Basil at 30% alpha
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        label = label?.let {
            {
                Text(
                    text = it,
                    color = labelColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal
                    )
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    color = placeholderColor,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        // Subtask 71: Underlined Input Style with 8dp corner radius
        shape = AppShapes.SmallShape, // 8dp corner radius from design system
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            errorCursorColor = MaterialTheme.colorScheme.error
        )
    )
}

/**
 * Enhanced password text field with visibility toggle
 */
@Composable
fun EnhancedPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = "Password",
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    EnhancedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = if (passwordVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingText = supportingText,
        isError = isError,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true
    )
}

/**
 * Enhanced search text field with clear functionality
 */
@Composable
fun EnhancedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Search recipes...",
    onSearch: ((String) -> Unit)? = null,
    onClear: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    
    EnhancedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = {
                        onValueChange("")
                        onClear?.invoke()
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
            capitalization = KeyboardCapitalization.None
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke(value)
                focusManager.clearFocus()
            }
        ),
        singleLine = true
    )
}

/**
 * Enhanced multiline text field for longer content
 */
@Composable
fun EnhancedTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    minLines: Int = 3,
    maxLines: Int = 8,
    maxLength: Int? = null
) {
    Column {
        EnhancedTextField(
            value = value,
            onValueChange = { newValue ->
                if (maxLength == null || newValue.length <= maxLength) {
                    onValueChange(newValue)
                }
            },
            modifier = modifier,
            enabled = enabled,
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences
            ),
            singleLine = false,
            minLines = minLines,
            maxLines = maxLines
        )
        
        // Character counter (if maxLength is specified)
        if (maxLength != null) {
            Text(
                text = "${value.length}/$maxLength",
                style = MaterialTheme.typography.labelSmall,
                color = if (value.length > maxLength * 0.9f) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 16.dp)
                    .wrapContentWidth(androidx.compose.ui.Alignment.End)
            )
        }
    }
}

/**
 * Enhanced numeric input field
 */
@Composable
fun EnhancedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    isDecimal: Boolean = false
) {
    EnhancedTextField(
        value = value,
        onValueChange = { newValue ->
            // Filter input to only allow numbers and decimal point (if enabled)
            val filteredValue = if (isDecimal) {
                newValue.filter { it.isDigit() || it == '.' }
                    .let { filtered ->
                        // Ensure only one decimal point
                        val decimalIndex = filtered.indexOf('.')
                        if (decimalIndex != -1) {
                            filtered.substring(0, decimalIndex + 1) + 
                            filtered.substring(decimalIndex + 1).filter { it.isDigit() }
                        } else {
                            filtered
                        }
                    }
            } else {
                newValue.filter { it.isDigit() }
            }
            onValueChange(filteredValue)
        },
        modifier = modifier,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        singleLine = true
    )
}

/**
 * Preview composables for the enhanced text fields
 */
@Preview(name = "Enhanced Text Field - Basic")
@Composable
private fun EnhancedTextFieldPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedTextField(
                    value = "",
                    onValueChange = {},
                    label = "Recipe Title",
                    placeholder = "Enter recipe title..."
                )
                
                EnhancedTextField(
                    value = "Delicious Pasta",
                    onValueChange = {},
                    label = "Recipe Title",
                    placeholder = "Enter recipe title..."
                )
            }
        }
    }
}

@Preview(name = "Enhanced Text Field - States")
@Composable
private fun EnhancedTextFieldStatesPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedTextField(
                    value = "",
                    onValueChange = {},
                    label = "Normal State",
                    placeholder = "Type something..."
                )
                
                EnhancedTextField(
                    value = "Error input",
                    onValueChange = {},
                    label = "Error State",
                    placeholder = "Type something...",
                    isError = true,
                    supportingText = { Text("This field has an error") }
                )
                
                EnhancedTextField(
                    value = "Disabled input",
                    onValueChange = {},
                    label = "Disabled State",
                    placeholder = "Type something...",
                    enabled = false
                )
            }
        }
    }
}

@Preview(name = "Enhanced Special Fields")
@Composable
private fun EnhancedSpecialFieldsPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedPasswordField(
                    value = "password123",
                    onValueChange = {},
                    label = "Password"
                )
                
                EnhancedSearchField(
                    value = "pasta recipes",
                    onValueChange = {},
                    placeholder = "Search recipes..."
                )
                
                EnhancedNumberField(
                    value = "4",
                    onValueChange = {},
                    label = "Servings",
                    placeholder = "Enter number of servings"
                )
                
                EnhancedNumberField(
                    value = "2.5",
                    onValueChange = {},
                    label = "Prep Time (hours)",
                    placeholder = "Enter prep time",
                    isDecimal = true
                )
            }
        }
    }
}

@Preview(name = "Enhanced Text Area")
@Composable
private fun EnhancedTextAreaPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedTextArea(
                    value = "This is a delicious pasta recipe that combines fresh tomatoes, basil, and garlic to create an amazing flavor profile.",
                    onValueChange = {},
                    label = "Recipe Description",
                    placeholder = "Describe your recipe...",
                    maxLength = 500
                )
            }
        }
    }
}

@Preview(name = "Enhanced Text Fields - Dark Theme")
@Composable
private fun EnhancedTextFieldDarkPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedTextField(
                    value = "",
                    onValueChange = {},
                    label = "Recipe Title",
                    placeholder = "Enter recipe title..."
                )
                
                EnhancedPasswordField(
                    value = "",
                    onValueChange = {},
                    label = "Password"
                )
                
                EnhancedSearchField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Search recipes..."
                )
            }
        }
    }
} 