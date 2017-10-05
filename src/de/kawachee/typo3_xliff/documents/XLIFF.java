package de.kawachee.typo3_xliff.documents;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import de.kawachee.typo3_xliff.exceptions.BodyNotWriteableException;
import de.kawachee.typo3_xliff.exceptions.InvalidXliffFileException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fabian Bettag
 */
public class XLIFF {

    private XmlTag bodySubTag;

    public XLIFF(PsiFile file) throws InvalidXliffFileException {
        try {
            XmlFile xmlFile = (XmlFile) file;

            assertValidity(xmlFile);

            bodySubTag = xmlFile.getRootTag().findFirstSubTag("file").findFirstSubTag("body");
        } catch (ClassCastException castException) {
            Messages.showErrorDialog("You need to associate '.xlf' files with the XML filetype", "Error");
        }
    }

    public void translate(Pair<String, Boolean> input, String value, boolean createTarget) throws BodyNotWriteableException {
        if (!this.bodySubTag.isWritable() && !this.bodySubTag.isValid()) {
            throw new BodyNotWriteableException();
        }

        String id = input.getFirst();
        XmlTag tag = findById(id);

        if (tag == null) {
            tag = createTag(id, input.getSecond());
        }

        tag.findFirstSubTag("source").getValue().setText(value);

        if (createTarget) {
            createOrUpdateTarget(value, tag);
        }

        tag.setAttribute("approved", "no");
    }

    private void createOrUpdateTarget(String value, XmlTag root) {
        XmlTag target = root.findFirstSubTag("target");
        if (target == null) {
            target = root.createChildTag("target", bodySubTag.getNamespace(), "", false);
            target.getValue().setText(value);
            root.addSubTag(target, false);
        }

        target.getValue().setText(value);
    }

    @NotNull
    private XmlTag createTag(String id, Boolean preserveSpace) {
        XmlTag tag = bodySubTag.createChildTag("trans-unit", bodySubTag.getNamespace(), null, false);
        tag.setAttribute("id", id);

        if (preserveSpace) {
            tag.setAttribute("space", "xml", "preserve");
        }

        XmlTag sourceTag = tag.createChildTag("source", bodySubTag.getNamespace(), "", false);
        tag.addSubTag(sourceTag, false);

        return bodySubTag.addSubTag(tag, false);
    }

    @Nullable
    private XmlTag findById(String id) {
        for (XmlTag xmlTag : bodySubTag.getSubTags()) {
            if (xmlTag.getAttribute("id").getValue().equals(id)) {
                return xmlTag;
            }
        }
        return null;
    }

    private void assertValidity(XmlFile document) throws InvalidXliffFileException {
        XmlTag root = document.getRootTag();
        if (root == null) {
            throw new InvalidXliffFileException("xliff");
        }

        XmlTag file = root.findFirstSubTag("file");
        if (file == null) {
            throw new InvalidXliffFileException("file");
        }

        if (file.findFirstSubTag("body") == null) {
            throw new InvalidXliffFileException("body");
        }
    }

}
