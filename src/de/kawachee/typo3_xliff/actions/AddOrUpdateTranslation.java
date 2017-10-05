package de.kawachee.typo3_xliff.actions;

import com.intellij.codeInsight.actions.AbstractLayoutCodeProcessor;
import com.intellij.codeInsight.actions.RearrangeCodeProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import de.kawachee.typo3_xliff.documents.XLIFF;
import de.kawachee.typo3_xliff.exceptions.BodyNotWriteableException;
import de.kawachee.typo3_xliff.exceptions.InvalidXliffFileException;

import java.io.IOException;

public class AddOrUpdateTranslation extends AbstractAction {

    protected boolean preserveSpaces = false;

    private void updateTranslationDocument(final Pair<String, Boolean> unitId, final String unitValue) throws IOException, InvalidXliffFileException {
        final XLIFF xliffDocument;
        xliffDocument = new XLIFF(selectedFile);

        final String value = transformer.transformValue(unitValue);

        WriteCommandAction.runWriteCommandAction(selectedFile.getProject(), new Runnable() {
            @Override
            public void run() {
                String filename = selectedFile.getVirtualFile().getName();

                Project project = selectedFile.getProject();
                try {
                    xliffDocument.translate(unitId, value, filename.matches("[a-z]{1,2}?\\.locallang\\.xlf"));
                } catch (BodyNotWriteableException e) {
                    Notifications.Bus.notify(notificationGroup.createNotification("The languange file is not writeable", MessageType.ERROR));
                }
                reformatDocument(project, selectedFile);
                saveDocument(project);
            }
        });
    }

    private void replaceSelectedTextWithViewHelper(final String translationKeyId, final Project project, final Editor editor, boolean hasArguments) {

        final String templateString = transformer.replacement(hasArguments);

        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                final int selectionStart = editor.getSelectionModel().getSelectionStart();
                final int selectionEnd = editor.getSelectionModel().getSelectionEnd();

                final String replacement = String.format(templateString, translationKeyId);
                final Document editorDocument = editor.getDocument();

                if (!editorDocument.isWritable()) {
                    editorDocument.setReadOnly(false);
                }

                editorDocument.replaceString(selectionStart, selectionEnd, replacement);
                FileDocumentManager.getInstance().saveDocument(editorDocument);
            }
        });
    }

    @Override
    protected void doAction(AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final String selectedText = editor.getSelectionModel().getSelectedText();
        final Project project = e.getProject();
        final Pair<String, Boolean> userInputPair = Messages.showInputDialogWithCheckBox("Translation key",
                "Translation key", "Preserve space?",
                preserveSpaces, true, Messages.getQuestionIcon(), "", new NonEmptyInputValidator());

        preserveSpaces = userInputPair.getSecond();
        if (selectedFile != null && userInputPair.getFirst() != null) {
            try {
                updateTranslationDocument(userInputPair, selectedText);
                replaceSelectedTextWithViewHelper(userInputPair.getFirst(), project, editor, selectedText.contains("%s"));
            } catch (IOException | InvalidXliffFileException ex) {
                notificationGroup.createNotification(ex.getMessage(), NotificationType.ERROR)
                        .notify(project);
            }

        }
    }

    private void reformatDocument(Project project, PsiFile file) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        AbstractLayoutCodeProcessor codeProcessor = new ReformatCodeProcessor(project, file, null, true);
        codeProcessor = new RearrangeCodeProcessor(codeProcessor);
        codeProcessor.run();
    }

    private void saveDocument(Project project) {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        Document document = psiDocumentManager.getDocument(selectedFile);

        // make sure all changes are applied to the document before save
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(document);

        FileDocumentManager.getInstance().saveDocument(document);
    }

}
