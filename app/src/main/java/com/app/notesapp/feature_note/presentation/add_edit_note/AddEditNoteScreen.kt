package com.app.notesapp.feature_note.presentation.add_edit_note

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.notesapp.feature_note.domain.model.Note
import com.app.notesapp.feature_note.presentation.add_edit_note.components.ShareDialog
import com.app.notesapp.feature_note.presentation.add_edit_note.components.TransparentHintTextField
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddEditNoteScreen(
    navController: NavController,

    noteColor: Int,
    viewModel: AddEditNoteViewModel = hiltViewModel()
) {
    val titleState = viewModel.noteTitle.value
    val contentState = viewModel.noteContent.value

    val snackBarHostState = remember {
        SnackbarHostState()
    }

    val noteBackgroundAnimatable = remember {
        Animatable(
            Color(if (noteColor != -1) noteColor else viewModel.noteColor.value)
        )
    }
    val scope = rememberCoroutineScope()
    val localView = LocalView.current
    val capturedBitmap = remember { mutableStateOf<Bitmap?>(null) }



    LaunchedEffect(key1 = true) {
        viewModel.evenFlow.collectLatest { event ->
            when (event) {
                is AddEditNoteViewModel.UiEvent.ShowSnackbar -> {
                    snackBarHostState.showSnackbar(
                        message = event.message
                    )
                }

                is AddEditNoteViewModel.UiEvent.SaveNote -> {
                    navController.navigateUp()
                }

                is AddEditNoteViewModel.UiEvent.ShareText -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share note")
                    navController.context.startActivity(shareIntent)
                }

                is AddEditNoteViewModel.UiEvent.ShareDoc -> {
                    val context = navController.context

                    val fileName = "note_${System.currentTimeMillis()}.docx"
                    val file = File(context.cacheDir, fileName)

                    val content = event.text

                    // ✔ Create real Word document using Apache POI
                    val document = org.apache.poi.xwpf.usermodel.XWPFDocument()
                    val para = document.createParagraph()
                    val run = para.createRun()
                    run.setText(content)

                    file.outputStream().use { out ->
                        document.write(out)
                    }
                    document.close()

                    val uri = FileProvider.getUriForFile(
                        context,
                        context.packageName + ".provider",
                        file
                    )

                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type =
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Word File"))
                }


                is AddEditNoteViewModel.UiEvent.ShareImage -> {
                    val activity = navController.context as Activity

                    // 1️⃣ Capture bitmap
                    val bitmap = captureComposeBitmap(activity.window.decorView.rootView)

                    // 2️⃣ Store PNG in cache
                    val file = File(activity.cacheDir, "note_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    // 3️⃣ Get URI using FileProvider
                    val uri = FileProvider.getUriForFile(
                        activity,
                        activity.packageName + ".provider",
                        file
                    )

                    // 4️⃣ Share Intent
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    activity.startActivity(Intent.createChooser(shareIntent, "Share Note Image"))
                }


            }
        }
    }





    if (viewModel.isShareDialogVisible.value) {
        ShareDialog(
            onShareText = {
                viewModel.onEvent(AddEditNoteEvent.ShareAsText)
            },
            onShareImage = {
                viewModel.onEvent(AddEditNoteEvent.ShareAsImage)
            },
            onShareDoc = {
                viewModel.onEvent(AddEditNoteEvent.ShareAsDoc)
            },
            onDismiss = {
                viewModel.onEvent(AddEditNoteEvent.HideShareDialog)
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState
            )
        },
        floatingActionButton = {
            Row {
                FloatingActionButton(
                    onClick = {
                        viewModel.onEvent(AddEditNoteEvent.ShowShareDialog)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share note")
                }

                FloatingActionButton(
                    onClick = {
                        viewModel.onEvent(AddEditNoteEvent.SaveNote)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Save note")
                }
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(noteBackgroundAnimatable.value)
                .padding(padding)
                .graphicsLayer()
                .padding(16.dp)
                .onGloballyPositioned {
                    capturedBitmap.value = captureComposeBitmap(localView)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Note.noteColors.forEach { color ->
                    val colorInt = color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(15.dp, CircleShape)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = 3.dp,
                                color = if (viewModel.noteColor.value == colorInt) {
                                    Color.Black
                                } else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                scope.launch {
                                    noteBackgroundAnimatable.animateTo(
                                        targetValue = Color(colorInt),
                                        animationSpec = tween(
                                            durationMillis = 500
                                        )
                                    )
                                }
                                viewModel.onEvent(
                                    AddEditNoteEvent.ChangeColor(
                                        colorInt
                                    )
                                )
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TransparentHintTextField(
                text = titleState.text,
                hint = titleState.hint,
                onValueChange = {
                    viewModel.onEvent(AddEditNoteEvent.EnteredTitle(it))
                },
                onFocusChange = {
                    viewModel.onEvent(AddEditNoteEvent.ChangeTitleFocus(it))
                },
                isHintVisible = titleState.isHintVisible,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            TransparentHintTextField(
                text = contentState.text,
                hint = contentState.hint,
                onValueChange = {
                    viewModel.onEvent(AddEditNoteEvent.EnteredContent(it))
                },
                onFocusChange = {
                    viewModel.onEvent(AddEditNoteEvent.ChangeContentFocus(it))
                },
                isHintVisible = contentState.isHintVisible,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

fun captureComposeBitmap(view: android.view.View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}