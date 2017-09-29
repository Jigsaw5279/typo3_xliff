package de.kawachee.typo3_xliff.actions;

import com.intellij.codeInsight.actions.AbstractLayoutCodeProcessor;
import com.intellij.codeInsight.actions.RearrangeCodeProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import de.kawachee.typo3_xliff.documents.XLIFF;
import de.kawachee.typo3_xliff.exceptions.InvalidXliffFileException;

import java.io.IOException;

public class AddOrUpdateTranslation extends AbstractAction {

    protected boolean preserveSpaces = false;

    private void updateTranslationDocument(final Pair<String, Boolean> unitId, final String unitValue) throws IOException, InvalidXliffFileException {
        final XLIFF xliffDocument = new XLIFF(selectedFile);

        StringBuilder temp = new StringBuilder(unitValue);
        switch (this.currentFile.getFileType().getName()) {
            case "PHP":
                char firstChar = temp.charAt(0);
                if(firstChar == '"' || firstChar == '\'') {
                    temp.deleteCharAt(0);
                }

                int end = temp.length() - 1;
                char lastChar = temp.charAt(end);
                if(lastChar == '"') {
                    temp.deleteCharAt(end);
                }
                break;
            case "HTML":
            case "PLAIN_TEXT":
            default:
                break;
        }

        final String value = temp.toString();

        WriteCommandAction.runWriteCommandAction(selectedFile.getProject(), new Runnable() {
            @Override
            public void run() {
                Project project = selectedFile.getProject();
                xliffDocument.translate(unitId, value);
                reformatDocument(project, selectedFile);
                saveDocument(project);
            }
        });
    }

    private void replaceSelectedTextWithViewHelper(final String translationKeyId, final Project project, final Editor editor, boolean hasArguments) {

        final String templateString;

        switch (this.currentFile.getFileType().getName()) {
            case "PHP":
                if(hasArguments) {
                    templateString = "LocalizationUtility::translate(\"%s\", $this->extensionName, []);";
                } else {
                    templateString = "LocalizationUtility::translate(\"%s\", $this->extensionName);";
                }
                break;
            case "HTML":
            case "PLAIN_TEXT":
                if(hasArguments) {
                    templateString = "<f:translate key=\"%s\" arguments=\"{}\" />";
                } else {
                    templateString = "<f:translate key=\"%s\" />";
                }
                break;
            default:
                templateString = "";
                break;
        }



        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                final int selectionStart = editor.getSelectionModel().getSelectionStart();
                final int selectionEnd = editor.getSelectionModel().getSelectionEnd();

                final String replacement = String.format(templateString, translationKeyId);
                final Document editorDocument = editor.getDocument();

                if(!editorDocument.isWritable()) {
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
            } catch (IOException | InvalidXliffFileException e1) {
                e1.printStackTrace();
                notificationGroup.createNotification(e1.getMessage(), NotificationType.ERROR)
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
