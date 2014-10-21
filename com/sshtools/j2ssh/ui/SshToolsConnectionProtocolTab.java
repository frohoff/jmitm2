/*
 *  Sshtools - Java SSH2 API
 *
 *  Copyright (C) 2002 Lee David Painter.
 *
 *  Written by: 2002 Lee David Painter <lee@sshtools.com>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.sshtools.j2ssh.ui;

import java.awt.*;
import javax.swing.*;
import com.sshtools.j2ssh.ui.*;
import java.util.ArrayList;
import java.util.Iterator;
import com.sshtools.j2ssh.transport.cipher.*;
import com.sshtools.j2ssh.transport.hmac.*;
import com.sshtools.j2ssh.transport.compression.*;
import com.sshtools.j2ssh.transport.publickey.*;
import com.sshtools.j2ssh.transport.kex.*;
import com.sshtools.j2ssh.util.*;

/**
 *  Implementation of a tab what allows editing of the protocol properties
 */
public class SshToolsConnectionProtocolTab extends JPanel implements SshToolsConnectionTab {

    final static String KEYS_ICON = "/com/sshtools/j2ssh/ui/largekeys.png";
    final static String PROTOCOL_ICON = "/com/sshtools/j2ssh/ui/largeprotocol.png";
    final static String DEFAULT = "<Default>";
    //
    protected JComboBox jComboCipherCS = new JComboBox();
    protected JComboBox jComboCipherSC = new JComboBox();
    protected JComboBox jComboMacCS = new JComboBox();
    protected JComboBox jComboMacSC = new JComboBox();
    protected JComboBox jComboCompCS = new JComboBox();
    protected JComboBox jComboCompSC = new JComboBox();
    protected JComboBox jComboKex = new JComboBox();
    protected JComboBox jComboPK = new JComboBox();
    protected SshToolsConnectionProfile profile;


    public SshToolsConnectionProtocolTab() {
        super();


    //  Keys
        JPanel keysPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;

    //  Public key
        UIUtil.jGridBagAdd(keysPanel,
            new JLabel("Public key"), gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(keysPanel, jComboPK, gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

    //  Public key
        UIUtil.jGridBagAdd(keysPanel,
            new JLabel("Key exchange"), gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(keysPanel, jComboKex, gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

    //
        IconWrapperPanel iconKeysPanel = new IconWrapperPanel(
            new ResourceIcon(KEYS_ICON), keysPanel);

    //  Preferences
        JPanel prefPanel = new JPanel(new GridBagLayout());
        prefPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;

    //  Public key
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Client - > Server"), gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Server - > Client"), gbc, GridBagConstraints.REMAINDER);

    //  Separator
        gbc.weightx = 2.0;
        UIUtil.jGridBagAdd(prefPanel, new JSeparator(JSeparator.HORIZONTAL),
                gbc, GridBagConstraints.REMAINDER);

    //  Cipher
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Cipher"), gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Cipher"), gbc, GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(prefPanel,
            jComboCipherCS, gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(prefPanel,
            jComboCipherSC, gbc, GridBagConstraints.REMAINDER);

    //  Mac
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Mac"), gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Mac"), gbc, GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(prefPanel,
            jComboMacCS, gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(prefPanel,
            jComboMacSC, gbc, GridBagConstraints.REMAINDER);

    //  Compression
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Compression"), gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(prefPanel,
            new JLabel("Compression"), gbc, GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(prefPanel,
            jComboCompCS, gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(prefPanel,
            jComboCompSC, gbc, GridBagConstraints.REMAINDER);

    //
        IconWrapperPanel iconPrefPanel = new IconWrapperPanel(
            new ResourceIcon(PROTOCOL_ICON), prefPanel);

    //  This tab
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;
        UIUtil.jGridBagAdd(this, iconKeysPanel,
                                        gbc, GridBagConstraints.REMAINDER);
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(this, iconPrefPanel,
                                        gbc, GridBagConstraints.REMAINDER);

    //
        loadList(SshCipherFactory.getSupportedCiphers(), jComboCipherCS, true);
        loadList(SshCipherFactory.getSupportedCiphers(), jComboCipherSC, true);
        loadList(SshHmacFactory.getSupportedMacs(), jComboMacCS, true);
        loadList(SshHmacFactory.getSupportedMacs(), jComboMacSC, true);
        loadList(SshCompressionFactory.getSupportedCompression(), jComboCompCS, true);
        loadList(SshCompressionFactory.getSupportedCompression(), jComboCompSC, true);
        loadList(SshKeyExchangeFactory.getSupportedKeyExchanges(), jComboKex, true);
        loadList(SshKeyPairFactory.getSupportedKeys(), jComboPK, true);
    }

    public void setConnectionProfile(SshToolsConnectionProfile profile) {
        this.profile = profile;
    }

    public SshToolsConnectionProfile getConnectionProfile() {
        return profile;
    }

    /**
     *  Loads a list into a combo box
     *
     *@param  list        The source list
     *@param  combo       The destination combo box
     *@param  addDefault  Whether to add the <Default> item
     */
    private void loadList(java.util.List list, JComboBox combo, boolean addDefault) {
        Iterator it = list.iterator();
        if (addDefault) {
            combo.addItem(DEFAULT);
        }
        while (it.hasNext()) {
            combo.addItem(it.next());
        }
    }

    public String getTabContext() {
        return "Connection";
    }

    public Icon getTabIcon() {
        return null;
    }

    public String getTabTitle() {
        return "Protocol";
    }

    public String getTabToolTipText() {
        return "Protocol related properties.";
    }

    public int getTabMnemonic() {
        return 'p';
    }

    public Component getTabComponent() {
        return this;
    }

    public boolean validateTab() {
        return true;
    }

    public void applyTab() {
        // Get the algorithm preferences
        if (!jComboCipherCS.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefCSEncryption((String) jComboCipherCS.getSelectedItem());
        }
        if (!jComboCipherSC.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefSCEncryption((String) jComboCipherSC.getSelectedItem());
        }
        if (!jComboMacCS.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefCSMac((String) jComboMacCS.getSelectedItem());
        }
        if (!jComboMacSC.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefSCMac((String) jComboMacSC.getSelectedItem());
        }
        if (!jComboCompCS.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefCSComp((String) jComboCompCS.getSelectedItem());
        }
        if (!jComboCompSC.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefSCComp((String) jComboCompSC.getSelectedItem());
        }
        if (!jComboKex.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefKex((String) jComboKex.getSelectedItem());
        }
        if (!jComboPK.getSelectedItem().equals(DEFAULT)) {
            profile.setPrefPublicKey((String) jComboPK.getSelectedItem());
        }
    }

    public void tabSelected() {
    }
}
