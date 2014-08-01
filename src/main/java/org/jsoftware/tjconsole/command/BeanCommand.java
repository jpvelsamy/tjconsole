package org.jsoftware.tjconsole.command;

import jline.console.completer.Completer;
import org.jsoftware.tjconsole.Output;
import org.jsoftware.tjconsole.TJContext;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Select mxBean to connect to.
 *
 * @author szalik
 */
public class BeanCommand extends AbstractCommand implements Completer {
    private static final String PREFIX = "\\b";


    public BeanCommand(TJContext ctx, Output output) {
        super(ctx, output);
    }


    @Override
    public void action(String input) throws Exception {
        StringBuilder sb = new StringBuilder();
        String bName = extractURL(input);
        if (bName.length() == 0) {
            for (String bn : names()) {
                sb.append("\t* ").append(bn).append('\n');
                output.outInfo(sb.toString());
            }
        } else {
            output.outInfo("Connecting to bean " + bName + "...");
            ObjectName objectName = new ObjectName(bName);
            if (ctx.getServer().isRegistered(objectName)) {
                output.outInfo("Connected to bean " + bName);
                ctx.setObjectName(objectName);
            } else {
                output.outError("Bean " + bName + " not found.");
                ctx.setObjectName(null);
            }
        }
    }


    @Override
    public boolean matches(String input) {
        return input.startsWith(PREFIX);
    }


    @SuppressWarnings("unchecked")
    public int complete(String buffer, int cursor, List candidates) {
        if (matches(buffer) && ctx.isConnected()) {
            String urlPrefix = extractURL(buffer);
            try {
                for (String s : names()) {
                    if (s.startsWith(urlPrefix)) {
                        candidates.add(s);
                    }
                }
            } catch (IOException e) { /* FIXME do not leave empty catch statements, at least log it using logger */ }
            return PREFIX.length();
        } else {
            return -1;
        }
    }


    @Override
    public CmdDescription getHelp() {
        return new CmdDescription("Select bean.", "\\b beanName", "\\b") {
            private static final long serialVersionUID = 7806368129076996728L;

            @Override
            public boolean isProper(TJContext ctx) {
                return ctx.isConnected();
            }
        };
    }


    private static String extractURL(String in) {
        return in.substring(PREFIX.length()).trim();
    }


    private Collection<String> names() throws IOException {
        ArrayList<String> l = new ArrayList<String>();
        for (Object on : ctx.getServer().queryNames(null, null)) {
            l.add(on.toString());
        }
        return l;
    }

}
