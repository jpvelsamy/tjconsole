package org.jsoftware.tjconsole.local;

import com.sun.tools.attach.*;

import java.io.File;
import java.util.*;

import sun.management.ConnectorAddressLink;

import javax.management.remote.JMXServiceURL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Util to resolve Java PIDs and resolve mxBeanServer URL by PID
 */
public class ProcessListManager {
    private final static Pattern SPLITTER = Pattern.compile("[:\\- ]");

    ProcessListManager() {
    }

    public Collection<JvmPid> getLocalProcessList() {
        List<VirtualMachineDescriptor> localVms = VirtualMachine.list();
        List<JvmPid> out = new ArrayList<JvmPid>();
        for (VirtualMachineDescriptor vmd: localVms) {
            out.add(new JvmPid(vmd.id(), vmd.displayName()));
        }
        return out;
    }




    /**
     * @param url string of local mxBeanServer url
     * @return MxBeanServer URL for local process
     * @throws IOException
     * @see #getLocalProcessList()
     */
    public JMXServiceURL getLocalServiceURL(String url) throws IOException, AgentLoadException, AgentInitializationException, LocalJvmAttachException {
        String pid = url.trim().substring(ProcessListManagerLoader.LOCAL_PREFIX.length()).trim();
        Matcher matcher = SPLITTER.matcher(pid);
        if (matcher.find()) {
            int index = matcher.start();
            pid = pid.substring(0, index);
        }
        pid = pid.trim();
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (connectorAddress == null) { // no JMX - enable it
                Properties remoteProperties = vm.getSystemProperties();
                String separator = remoteProperties.getProperty("file.separator", File.separator);
                StringBuilder agent = new StringBuilder(remoteProperties.getProperty("java.home")).append(separator).append("lib").append(separator).append("management-agent.jar");
                vm.loadAgent(agent.toString());
                connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            }
            return new JMXServiceURL(connectorAddress);
        } catch (AttachNotSupportedException ex) {
            throw new LocalJvmAttachException(ex.getMessage());
        }
    }

}
