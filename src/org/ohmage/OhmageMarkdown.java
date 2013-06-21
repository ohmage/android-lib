/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ohmage;

import android.text.Html;

import org.markdownj.CharacterProtector;
import org.markdownj.HTMLToken;
import org.markdownj.MarkdownProcessor;
import org.markdownj.TextEditor;

import java.util.Collection;
import java.util.Iterator;

/**
 * Parser for ohmage Markdown. We just use a subset of the functionality
 * provided by the {@link MarkdownProcessor}
 * 
 * @author cketcham
 */
public class OhmageMarkdown {

    private final CharacterProtector CHAR_PROTECTOR = new CharacterProtector();

    /**
     * Parses string to SpannableString. Converts ** to bold and * to italic.
     * 
     * @param label
     * @return SpannableString
     */
    public static CharSequence parse(String label) {
        return Html.fromHtml(parseHtml(label).toString());
    }

    /**
     * Parses string to HTML. Converts ** to bold and * to italic.
     * 
     * @param label
     * @return HTML string
     */
    public static String parseHtml(String label) {
        OhmageMarkdown markdown = new OhmageMarkdown();
        TextEditor editor = new TextEditor(Utilities.getHtmlSafeDisplayString(label));
        doImages(editor);
        editor = markdown.escapeSpecialCharsWithinTagAttributes(editor);
        doItalicsAndBold(editor);
        markdown.unEscapeSpecialChars(editor);
        return editor.toString();
    }

    /**
     * escape special characters Within tags -- meaning between < and > --
     * encode [\ ` * _] so they don't conflict with their use in Markdown for
     * code, italics and strong. We're replacing each such character with its
     * corresponding random string value; this is likely overkill, but it should
     * prevent us from colliding with the escape values by accident.
     * 
     * @param text
     * @return
     */
    private TextEditor escapeSpecialCharsWithinTagAttributes(TextEditor text) {
        Collection tokens = text.tokenizeHTML();
        TextEditor newText = new TextEditor("");

        for (Iterator iterator = tokens.iterator(); iterator.hasNext();) {
            HTMLToken token = (HTMLToken) iterator.next();
            String value = "";
            value = token.getText();
            if (token.isTag()) {
                value = value.replaceAll("\\\\", CHAR_PROTECTOR.encode("\\"));
                value = value.replaceAll("`", CHAR_PROTECTOR.encode("`"));
                value = value.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
                value = value.replaceAll("_", CHAR_PROTECTOR.encode("_"));
            }
            newText.append(value);
        }

        return newText;
    }

    private void unEscapeSpecialChars(TextEditor ed) {
        for (Iterator iterator = CHAR_PROTECTOR.getAllEncodedTokens().iterator(); iterator
                .hasNext();) {
            String hash = (String) iterator.next();
            String plaintext = CHAR_PROTECTOR.decode(hash);
            ed.replaceAllLiteral(hash, plaintext);
        }
    }

    private static TextEditor doImages(TextEditor markup) {
        markup.replaceAll("!\\[(.*)\\]\\((.*) \"(.*)\"\\)",
                "<img src=\"$2\" alt=\"$1\" title=\"$3\" />");
        markup.replaceAll("!\\[(.*)\\]\\((.*)\\)", "<img src=\"$2\" alt=\"$1\" />");
        return markup;
    }

    private static TextEditor doItalicsAndBold(TextEditor markup) {
        markup.replaceAll("(\\*\\*|__)(?=\\S)(.+?[*_]*)(?<=\\S)\\1", "<strong>$2</strong>");
        markup.replaceAll("(\\*|_)(?=\\S)(.+?)(?<=\\S)\\1", "<em>$2</em>");
        return markup;
    }
}
