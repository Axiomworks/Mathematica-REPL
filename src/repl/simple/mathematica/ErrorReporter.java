/*
    Mathematica REPL IntelliJ IDEA plugin
    Copyright (C) 2014  Aliaksandr Dubrouski

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package repl.simple.mathematica;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.Consumer;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SMTPSClient;
import org.apache.commons.net.smtp.SimpleSMTPHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.rmi.ConnectException;

/**
 * Allows sending bug reports to the author of plugin
 */
public class ErrorReporter extends ErrorReportSubmitter {
    private JPanel bugReportForm;


    @Override
    public String getReportActionText() {
        return MathREPLBundle.message("report");
    }

    public boolean submit(@NotNull IdeaLoggingEvent[] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<SubmittedReportInfo> consumer)
    {
     StringBuilder builder = new StringBuilder();
     for (IdeaLoggingEvent evt : events) builder.append(evt.getMessage());
     builder.append("\n");
     builder.append(additionalInfo);
     final boolean b = reportBug(builder.toString(), parentComponent);

     consumer.consume(new SubmittedReportInfo(null, "email", b ? SubmittedReportInfo.SubmissionStatus.NEW_ISSUE: SubmittedReportInfo.SubmissionStatus.FAILED));
     return b;
    }

    @Deprecated
    public SubmittedReportInfo submit(IdeaLoggingEvent[] events, Component parentComponent) {
        StringBuilder builder = new StringBuilder();
        for (IdeaLoggingEvent evt : events) builder.append(evt.getMessage());
        builder.append("\n");
        final boolean b = reportBug(builder.toString(), parentComponent);
        return new SubmittedReportInfo(null, "email", b ? SubmittedReportInfo.SubmissionStatus.NEW_ISSUE: SubmittedReportInfo.SubmissionStatus.FAILED);
    }

    static final class BugReportModel {
        String to;
        String cc;
        String mailserver;
        String mailuser;
        String message;
    }

    static class BugReportForm extends DialogWrapper {
        private JTextPane bugReportText;
        private JTextField mailUser;
        private JPanel myPanel;

        BugReportForm(String _bugReport, Component parent) {
            super(parent, true);

            setOKButtonText(MathREPLBundle.message("doSend"));
            setCancelButtonText(MathREPLBundle.message("notSend"));
            setModal(true);

            if (_bugReport != null && _bugReport.length() > 0) {
                bugReportText.setText(_bugReport);
            } else {
                bugReportText.setVisible(false);
            }

            mailUser.setText("noreply");

            init();
            pack();
        }

        protected void doOKAction() {
            super.doOKAction();

        }

        @Nullable
        protected JComponent createCenterPanel() {
            return myPanel;
        }

        {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
            $$$setupUI$$$();
        }

        /**
         * Method generated by IntelliJ IDEA GUI Designer
         * >>> IMPORTANT!! <<<
         * DO NOT edit this method OR call it in your code!
         *
         * @noinspection ALL
         */
        private void $$$setupUI$$$() {
            myPanel = new JPanel();
            myPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
            final JScrollPane scrollPane1 = new JScrollPane();
            myPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
            scrollPane1.setBorder(BorderFactory.createTitledBorder("Bug report"));
            bugReportText = new JTextPane();
            bugReportText.setMaximumSize(new Dimension(200, 200));
            scrollPane1.setViewportView(bugReportText);
            mailUser = new JTextField();
            mailUser.setText("noreply");
            myPanel.add(mailUser, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
            final JLabel label1 = new JLabel();
            label1.setText("Your Email:");
            myPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            label1.setLabelFor(mailUser);
        }

        /**
         * @noinspection ALL
         */
        public JComponent $$$getRootComponent$$$() {
            return myPanel;
        }
    }

    /**
     * Sends the information to mail server.
     *
     * @param model of bug report
     */
    private static synchronized void sendBugData(BugReportModel model) {
        String subject;
        Writer writer;
        SimpleSMTPHeader header;
        SMTPSClient client;
        //
        subject  = MathREPLBundle.message("reportSubject");
        try
        {
            header = new SimpleSMTPHeader(model.mailuser, model.to, subject);

            client = new SMTPSClient();
            client.addProtocolCommandListener(new PrintCommandListener(
                                                  new PrintWriter(System.out), true));

            client.connect(model.mailserver);
            if (!SMTPReply.isPositiveCompletion(client.getReplyCode()))
            {
                client.disconnect();
                throw  new ConnectException(MathREPLBundle.message("smtpFailure"));
            }
            client.helo("localhost");
            if(client.execTLS()) {

                client.login();
                client.setSender(model.mailuser);
                client.addRecipient(model.to);

                writer = client.sendMessageData();

                if (writer != null) {
                    writer.write(header.toString());
                    writer.write(model.message);
                    writer.close();
                    client.completePendingCommand();
                }
            }
            client.logout();
            client.disconnect();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Reports a bug with given message
     *
     * @param message of bug description
     */
    public static boolean reportBug(String message, Component comp) {
        final String to = MathREPLBundle.message("authorEmail");

        StringBuffer buf = new StringBuffer(message.length() + 50);

        buf.append("Idea version:");
        buf.append(ApplicationInfo.getInstance().getFullVersion());
        buf.append('\n');

        buf.append("Plugin version:");
        buf.append(MathREPLBundle.message("pluginVersion"));
        buf.append('\n');

        buf.append(message);
        BugReportForm form = new BugReportForm(buf.toString(), comp);

        form.show();
        if (form.getExitCode() != DialogWrapper.OK_EXIT_CODE) return false;

        final BugReportModel model = new BugReportModel();

        model.to = to;
        // use restricted gmail server
        model.mailserver = "aspmx.l.google.com";
        model.mailuser = form.mailUser.getText();
        model.message = form.bugReportText.getText();

        sendBugData(model);

        return true;
    }
}
