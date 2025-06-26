package com.github.rrin.client.ui;
import com.github.rrin.dto.Group;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class AddGroupUI extends JFrame {

    private JPanel panel = new JPanel();
    private JLabel groupNameLabel, groupDescriptionLabel;
    private JTextArea groupNameField, groupDescriptionField;
    private JButton cancelButton,addGroupButton;
    private ApplicationFrame frame;
    private Warehouse warehouse;
    private Group editGroup;

    Color textColor = new Color(0x2F4052);
    Color mainColor = new Color(0xD8E1E9);
    Color secondColor = new Color(0xE7EBF1);

    public AddGroupUI(ApplicationFrame frame, Warehouse warehouse) {
        super("Create Group");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(400, 400);
        setLocationRelativeTo(null);

        this.warehouse = warehouse;
        this.frame = frame;

        addGroupUI();
        getContentPane().add(panel);
    }

    public AddGroupUI(Group editGroup, ApplicationFrame frame, Warehouse warehouse) {
        super("Edit group");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(400, 400);
        setLocationRelativeTo(null);

        this.warehouse = warehouse;
        this.frame = frame;
        this.editGroup = editGroup;

        addGroupUI(editGroup);
        getContentPane().add(panel);
    }

    private void addGroupUI() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(secondColor);

        addGroupName();
        addProductDescription();

        //колір фону полів вводу
        groupNameField.setBackground(mainColor);
        groupNameField.setForeground(textColor);
        groupDescriptionField.setBackground(mainColor);
        groupDescriptionField.setForeground(textColor);

        //додавання кнопок
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(secondColor);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        addButton(buttonPanel);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        cancelButton(buttonPanel);
        buttonPanel.add(Box.createHorizontalGlue());
        panel.add(buttonPanel);
    }

    private void addGroupUI(Group group) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(secondColor);

        addGroupName();
        addProductDescription();

        groupNameField.setText(group.name());
        groupNameField.setBackground(mainColor);
        groupNameField.setForeground(textColor);
        groupDescriptionField.setText(group.description());
        groupDescriptionField.setBackground(mainColor);
        groupDescriptionField.setForeground(textColor);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(secondColor);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        addButton(buttonPanel);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        cancelButton(buttonPanel);
        buttonPanel.add(Box.createHorizontalGlue());
        panel.add(buttonPanel);
    }

    private void addGroupName() {
        groupNameLabel = new JLabel("Group name:");
        groupNameLabel.setBackground(secondColor);
        groupNameLabel.setForeground(textColor);
        groupNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        groupNameLabel.setFont(new Font("Helvetica", Font.BOLD, 16));
        groupNameLabel.setBorder(new EmptyBorder(5, 0, 5,0));
        panel.add(groupNameLabel);

        //додавання поля вводу назви групи товарів
        groupNameField = new JTextArea(2,20);
        groupNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, groupNameField.getPreferredSize().height));
        groupNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        groupNameField.setFont(new Font("Helvetica", Font.ITALIC, 16));
        panel.add(groupNameField);
    }

    private void addProductDescription() {
        //додавання опису групи товарів
        groupDescriptionLabel = new JLabel("Description:");
        groupDescriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        groupDescriptionLabel.setBackground(secondColor);
        groupDescriptionLabel.setForeground(textColor);
        groupDescriptionLabel.setFont(new Font("Helvetica", Font.BOLD, 16));
        groupDescriptionLabel.setBorder(new EmptyBorder(5, 0, 5,0));
        panel.add(groupDescriptionLabel);

        groupDescriptionField = new JTextArea(5, 20);
        groupDescriptionField.setLineWrap(true);
        groupDescriptionField.setWrapStyleWord(true);
        groupDescriptionField.setMaximumSize(new Dimension(Integer.MAX_VALUE, groupDescriptionField.getPreferredSize().height));
        groupDescriptionField.setAlignmentX(Component.CENTER_ALIGNMENT);
        groupDescriptionField.setFont(new Font("Helvetica", Font.ITALIC, 16));
        JScrollPane scrollPane = new JScrollPane(groupDescriptionField);
        panel.add(scrollPane);
    }

    private void addButton(JPanel buttonPanel) {
        addGroupButton = new JButton("Create");
        addGroupButton.setBackground(mainColor);
        addGroupButton.setForeground(textColor);
        addGroupButton.setFont(new Font("Helvetica", Font.BOLD, 16));
        addGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (editGroup == null) {
                    try {
                        addProductGroup();
                        dispose();
                    } catch (Exception exception) {
                        JOptionPane.showMessageDialog(null, exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    editGroup();
                }

            }
        });
        buttonPanel.add(addGroupButton);
    }

    private void cancelButton(JPanel buttonPanel) {
        cancelButton = new JButton("Cancel");
        cancelButton.setBackground(mainColor);
        cancelButton.setForeground(textColor);
        cancelButton.setFont(new Font("Helvetica", Font.BOLD, 16));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
    }

    private void addProductGroup() throws Exception {
        String groupName = groupNameField.getText();
        String groupDescription = groupDescriptionField.getText();
        warehouse.addProductGroup(groupName, groupDescription);
        frame.updateGroupTable();

    }

    private void editGroup() {
        String groupName = groupNameField.getText();
        String groupDescription = groupDescriptionField.getText();

        if (groupName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Group name can't be empty", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            warehouse.editGroup(new Group(editGroup.id(), groupName, groupDescription));
            frame.updateGroupTable();
            dispose();
        }

    }
}