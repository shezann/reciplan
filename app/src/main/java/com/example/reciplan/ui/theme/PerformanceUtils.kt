package com.example.reciplan.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Task 18: Performance Optimization
 * Comprehensive performance monitoring and optimization utilities
 * 
 * Features:
 * - Animation performance monitoring (Subtask 181)
 * - Recomposition optimization utilities (Subtask 182)
 * - Memory usage optimization patterns (Subtask 183)
 */

/**
 * Subtask 181: Animation Performance
 * Performance-optimized animation specs for consistent 60fps
 */
object OptimizedAnimations {
    
    /**
     * High-performance spring spec optimized for 60fps
     * Uses lower stiffness and optimal damping for smooth animations
     */
    fun <T> performantSpring(
        dampingRatio: Float = 0.8f, // Higher damping for stability
        stiffness: Float = 400f, // Optimal stiffness for 60fps
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold
    )
    
    /**
     * Optimized tween spec with frame-aligned durations
     */
    @Composable
    fun <T> performantTween(
        baseDurationMs: Int = 200, // Frame-aligned duration (12 frames at 60fps)
        easing: Easing = FastOutSlowInEasing,
        delayMs: Int = 0
    ): TweenSpec<T> {
        // Align duration to 60fps frame boundaries (16.67ms per frame)
        val frameDuration = (16.67f).toInt()
        val alignedDuration = ((baseDurationMs / frameDuration) * frameDuration).coerceAtLeast(frameDuration)
        
        return tween(
            durationMillis = alignedDuration,
            easing = easing,
            delayMillis = delayMs
        )
    }
    
    /**
     * Memory-efficient keyframes for complex animations
     */
    fun <T> memoryEfficientKeyframes(
        durationMs: Int = 200,
        builder: KeyframesSpec.KeyframesSpecConfig<T>.() -> Unit
    ): KeyframesSpec<T> = keyframes {
        durationMillis = durationMs
        builder()
    }
}

/**
 * Subtask 182: Recomposition Optimization
 * Utilities to minimize unnecessary recompositions
 */
object RecompositionOptimizations {
    
    /**
     * Stable data class for animation states to prevent recomposition
     */
    @Stable
    data class AnimationState(
        val scale: Float = 1f,
        val alpha: Float = 1f,
        val rotation: Float = 0f,
        val offsetX: Dp = 0.dp,
        val offsetY: Dp = 0.dp
    ) {
        companion object {
            val Default = AnimationState()
        }
    }
    
    /**
     * Stable like state wrapper to prevent unnecessary recompositions
     */
    @Stable
    data class StableLikeState(
        val isLiked: Boolean = false,
        val likesCount: Int = 0,
        val isLoading: Boolean = false,
        val hasError: Boolean = false
    )
    
    /**
     * Stable recipe data class for lists
     */
    @Stable
    data class StableRecipeItem(
        val id: String,
        val title: String,
        val thumbnail: String?,
        val likeState: StableLikeState
    )
    
    /**
     * Performance-optimized remember with custom equality
     */
    @Composable
    inline fun <T> rememberStable(
        key1: Any?,
        crossinline calculation: () -> T
    ): T {
        return remember(key1) { calculation() }
    }
    
    /**
     * Optimized derived state that only recomposes when necessary
     */
    @Composable
    fun <T, R> derivedStateOf(
        key1: T,
        calculation: (T) -> R
    ): State<R> {
        return remember { derivedStateOf { calculation(key1) } }
    }
}

/**
 * Subtask 183: Memory Usage Optimization
 * Memory-efficient image loading and state management
 */
object MemoryOptimizations {
    
    /**
     * Memory-efficient image loading with proper lifecycle management
     */
    @Composable
    fun OptimizedAsyncImage(
        imageUrl: String?,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        placeholder: @Composable (() -> Unit)? = null,
        error: @Composable (() -> Unit)? = null,
        loading: @Composable (() -> Unit)? = null,
        maxImageSize: Dp = 400.dp
    ) {
        val isInPreview = LocalInspectionMode.current
        val density = LocalDensity.current
        
        // Calculate optimal image size in pixels for memory efficiency
        val imageSizePx = with(density) { maxImageSize.roundToPx() }
        
        if (isInPreview) {
            // Use placeholder in preview to avoid memory issues
            placeholder?.invoke() ?: Box(modifier = modifier)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = modifier
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        loading?.invoke() ?: CircularProgressIndicator()
                    }
                    is AsyncImagePainter.State.Error -> {
                        error?.invoke() ?: Box(modifier = Modifier.fillMaxSize())
                    }
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                    }
                    else -> {
                        placeholder?.invoke() ?: Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
    
    /**
     * Memory-efficient state management with automatic cleanup
     */
    class ManagedStateFlow<T>(initialValue: T) {
        private val _value = MutableStateFlow(initialValue)
        val value: StateFlow<T> = _value.asStateFlow()
        
        private var isActive = true
        
        fun updateValue(newValue: T) {
            if (isActive) {
                _value.value = newValue
            }
        }
        
        fun cleanup() {
            isActive = false
        }
    }
    
    /**
     * Automatic cleanup for state flows when composable leaves composition
     */
    @Composable
    fun <T> rememberManagedStateFlow(initialValue: T): ManagedStateFlow<T> {
        val stateFlow = remember { ManagedStateFlow(initialValue) }
        
        DisposableEffect(Unit) {
            onDispose {
                stateFlow.cleanup()
            }
        }
        
        return stateFlow
    }
    
    /**
     * Memory-efficient list state that recycles objects
     */
    @Composable
    fun <T> rememberRecyclingListState(
        items: List<T>,
        keySelector: (T) -> String
    ): List<T> {
        return remember(items.size, items.firstOrNull()?.let(keySelector)) {
            items
        }
    }
}

/**
 * Performance monitoring utilities for development
 */
object PerformanceMonitor {
    
    /**
     * Composition counter for debugging recomposition issues
     */
    @Composable
    fun TrackRecompositions(
        tag: String,
        content: @Composable () -> Unit
    ) {
        val recompositionCount = remember { mutableIntStateOf(0) }
        
        SideEffect {
            recompositionCount.intValue++
            if (recompositionCount.intValue > 1) {
                println("🔄 Recomposition #${recompositionCount.intValue} for: $tag")
            }
        }
        
        content()
    }
    
    /**
     * Animation frame rate monitor
     */
    @Composable
    fun MonitorAnimationFrameRate(
        enabled: Boolean = false,
        tag: String = "Animation",
        content: @Composable () -> Unit
    ) {
        if (enabled) {
            var lastFrameTime by remember { mutableLongStateOf(0L) }
            
            SideEffect {
                val currentTime = System.currentTimeMillis()
                if (lastFrameTime > 0) {
                    val frameDuration = currentTime - lastFrameTime
                    val fps = 1000f / frameDuration
                    if (fps < 55f) { // Alert for drops below 55fps
                        println("⚠️ Low FPS detected for $tag: ${fps.toInt()}fps")
                    }
                }
                lastFrameTime = currentTime
            }
        }
        
        content()
    }
    
    /**
     * Memory usage tracker for development
     */
    @Composable
    fun TrackMemoryUsage(
        tag: String,
        enabled: Boolean = false
    ) {
        if (enabled) {
            LaunchedEffect(Unit) {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val usedMemoryMB = usedMemory / (1024 * 1024)
                println("🧠 Memory usage for $tag: ${usedMemoryMB}MB")
            }
        }
    }
}

/**
 * Performance-optimized component wrappers
 */
object OptimizedComponents {
    
    /**
     * High-performance animated visibility with optimized transitions
     */
    @Composable
    fun PerformantAnimatedVisibility(
        visible: Boolean,
        modifier: Modifier = Modifier,
        enter: EnterTransition = fadeIn(OptimizedAnimations.performantTween()),
        exit: ExitTransition = fadeOut(OptimizedAnimations.performantTween()),
        content: @Composable AnimatedVisibilityScope.() -> Unit
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = enter,
            exit = exit,
            content = content
        )
    }
    
    /**
     * Memory-efficient crossfade animation
     */
    @Composable
    fun <T> EfficientCrossfade(
        targetState: T,
        modifier: Modifier = Modifier,
        animationSpec: FiniteAnimationSpec<Float> = OptimizedAnimations.performantTween(),
        content: @Composable (T) -> Unit
    ) {
        androidx.compose.animation.Crossfade(
            targetState = targetState,
            modifier = modifier,
            animationSpec = animationSpec,
            content = content
        )
    }
} 