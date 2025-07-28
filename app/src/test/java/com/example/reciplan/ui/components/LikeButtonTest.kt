package com.example.reciplan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.reciplan.ui.theme.ReciplanTheme
import org.junit.Rule
import org.junit.Test

class LikeButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun likeButton_displaysCorrectIcon_whenUnliked() {
        var isLiked = false
        
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = isLiked,
                    likesCount = 42,
                    onClick = { isLiked = !isLiked }
                )
            }
        }

        // Should show unfilled heart icon when not liked
        composeTestRule
            .onNodeWithContentDescription("Like. Currently 42 likes")
            .assertExists()
    }

    @Test
    fun likeButton_displaysCorrectIcon_whenLiked() {
        var isLiked = true
        
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = isLiked,
                    likesCount = 43,
                    onClick = { isLiked = !isLiked }
                )
            }
        }

        // Should show filled heart icon when liked
        composeTestRule
            .onNodeWithContentDescription("Unlike. Currently 43 likes")
            .assertExists()
    }

    @Test
    fun likeButton_triggersOnClick_whenClicked() {
        var clickCount = 0
        
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    onClick = { clickCount++ }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Like. Currently 42 likes")
            .performClick()

        assert(clickCount == 1)
    }

    @Test
    fun likeButton_doesNotTriggerOnClick_whenDisabled() {
        var clickCount = 0
        
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    enabled = false,
                    onClick = { clickCount++ }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Like. Currently 42 likes")
            .performClick()

        // Should not have triggered click when disabled
        assert(clickCount == 0)
    }

    @Test
    fun likeButton_doesNotTriggerOnClick_whenLoading() {
        var clickCount = 0
        
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    isLoading = true,
                    onClick = { clickCount++ }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Like. Currently 42 likes")
            .performClick()

        // Should not have triggered click when loading
        assert(clickCount == 0)
    }

    @Test
    fun likeButton_showsProgressIndicator_whenLoading() {
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    isLoading = true,
                    onClick = {}
                )
            }
        }

        // Should show progress indicator when loading
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun likeButton_formatsLikesCount_correctly() {
        composeTestRule.setContent {
            ReciplanTheme {
                Column {
                    LikeButton(
                        isLiked = false,
                        likesCount = 42,
                        onClick = {},
                        modifier = Modifier.testTag("button_42")
                    )
                    LikeButton(
                        isLiked = false,
                        likesCount = 1250,
                        onClick = {},
                        modifier = Modifier.testTag("button_1250")
                    )
                    LikeButton(
                        isLiked = false,
                        likesCount = 15000,
                        onClick = {},
                        modifier = Modifier.testTag("button_15000")
                    )
                    LikeButton(
                        isLiked = false,
                        likesCount = 1500000,
                        onClick = {},
                        modifier = Modifier.testTag("button_1500000")
                    )
                }
            }
        }

        // Verify different count formats
        composeTestRule.onNodeWithText("42").assertExists()
        composeTestRule.onNodeWithText("1.2K").assertExists()
        composeTestRule.onNodeWithText("15K").assertExists()
        composeTestRule.onNodeWithText("1.5M").assertExists()
    }

    @Test
    fun likeButton_hidesCount_whenShowCountIsFalse() {
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    showCount = false,
                    onClick = {}
                )
            }
        }

        // Should not show count text
        composeTestRule.onNodeWithText("42").assertDoesNotExist()
    }

    @Test
    fun likeButton_hasCorrectAccessibilityProperties() {
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Like. Currently 42 likes")
            .assert(hasSetAction(SemanticsProperties.Role to Role.Button))
    }

    @Test
    fun likeButton_usesCustomContentDescription_whenProvided() {
        val customDescription = "Custom like button description"
        
        composeTestRule.setContent {
            ReciplanTheme {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    contentDescription = customDescription,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(customDescription)
            .assertExists()
    }

    @Test
    fun animatedLikeButton_triggersOnClick_whenClicked() {
        var clickCount = 0
        
        composeTestRule.setContent {
            ReciplanTheme {
                AnimatedLikeButton(
                    isLiked = false,
                    likesCount = 42,
                    onClick = { clickCount++ }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Like. Currently 42 likes")
            .performClick()

        assert(clickCount == 1)
    }
} 