package de.kawachee.typo3_xliff.actions;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import de.kawachee.typo3_xliff.transfomers.Factory;
import de.kawachee.typo3_xliff.transfomers.Transformer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Fabian Bettag
 */
public abstract class AbstractAction extends AnAction {

    protected final NotificationGroup notificationGroup = NotificationGroup.toolWindowGroup("typo3_xliff-plugin", ToolWindowId.MESSAGES_WINDOW);

    protected final ArrayList<String> enabledFileTypes = new ArrayList<String>(Arrays.asList("PHP", "HTML", "PLAIN_TEXT"));

    protected PsiFile selectedFile;

    protected PsiFile currentFile;

    protected Transformer transformer;

    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();

        currentFile = actionEvent.getData(CommonDataKeys.PSI_FILE);

        if (selectedFile == null) {
            selectedFile = currentFile;
        }
        selectedFile = openFileChooserDialog(project);

        if(selectedFile != null) {
            try {
                transformer = Factory.build(currentFile.getFileType().getName());
                this.doAction(actionEvent);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException |InvocationTargetException e) {
                e.printStackTrace(System.err);
                Notifications.Bus.notify(notificationGroup.createNotification("Can't translate from here", MessageType.ERROR));
            }

        }
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);

        if (null == editor || null == file) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        String type = file.getFileType().getName();

        String selectedText = editor.getSelectionModel().getSelectedText();
        e.getPresentation().setEnabledAndVisible(
                    null != selectedText
                &&  this.enabledFileTypes.contains(type)
        );
    }

    @Nullable
    private PsiFile openFileChooserDialog(Project project) {
        FileChooserDialog fileChooserDialog = FileChooserFactory.getInstance().createFileChooser(FileChooserDescriptorFactory.createSingleFileDescriptor(), project, null);
        VirtualFile[] virtualFiles = fileChooserDialog.choose(project, selectedFile.getVirtualFile());

        if (virtualFiles.length > 0) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFiles[0]);
            if (file.isWritable() && file.isValid()) {
                return file;
            } else {
                Notifications.Bus.notify(notificationGroup.createNotification("The file is not writeable or not valid", MessageType.ERROR));
            }
        }
        return null;
    }

    protected abstract void doAction(AnActionEvent actionEvent);
}
