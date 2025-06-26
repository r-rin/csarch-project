package com.github.rrin.client.ui;
import com.github.rrin.dto.Group;
import com.github.rrin.dto.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class AddProductToGroupUI extends JFrame {
    private JPanel panel = new JPanel();
    private JLabel productNameLabel, productDescriptionLabel, productManufacturerLabel, productQuantityLabel, productPriceLabel;
    private JTextArea productNameField, productDescriptionField, productManufacturerField, productQuantityField, productPriceField;
    private JButton cancelButton, addProductButton;

    private Group groupAddTo;
    private Warehouse warehouse;
    private ApplicationFrame frame;
    private Product editProduct;

    Color textColor = new Color(0x2F4052);
    Color mainColor = new Color(0xD8E1E9);
    Color secondColor = new Color(0xE7EBF1);

    public AddProductToGroupUI(Group groupAddTo, Warehouse warehouse, ApplicationFrame frame) {
        super("Add Product To Group");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(600, 700);
        setLocationRelativeTo(null);

        addProductToGroupUI();
        getContentPane().add(panel);

        this.groupAddTo = groupAddTo;
        this.warehouse = warehouse;
        this.frame = frame;
    }

    public AddProductToGroupUI(Product product, Group productsGroup, Warehouse warehouse, ApplicationFrame mainFrame) {
        super("Edit Product");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(600, 700);
        setLocationRelativeTo(null);

        this.groupAddTo = productsGroup;
        this.warehouse = warehouse;
        this.frame = mainFrame;
        this.editProduct = product;
        addProductToGroupUI(editProduct);
        getContentPane().add(panel);
    }

    private void addProductToGroupUI(Product product) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(secondColor);

        addProductName();
        addProductDescription();
        addProductManufacturer();
        addProductQuantity();
        addProductPrice();

        JTextArea[] fields = {productNameField, productDescriptionField,
                productManufacturerField, productQuantityField,
                productPriceField};

        productNameField.setText(product.name());
        productDescriptionField.setText(product.description());
        productManufacturerField.setText(product.manufacturer());
        productQuantityField.setText(Integer.toString(product.quantity()));
        productPriceField.setText(Double.toString(product.price()));

        for (JTextArea field : fields) {
            field.setForeground(textColor);
            field.setBackground(mainColor);
            field.setMargin(new Insets(10, 5, 10, 5));
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(secondColor);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        addProductButton(buttonPanel);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        cancelButton(buttonPanel);
        buttonPanel.add(Box.createHorizontalGlue());
        panel.add(buttonPanel);
    }

    private void addProductToGroupUI() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(secondColor);

        addProductName();
        addProductDescription();
        addProductManufacturer();
        addProductQuantity();
        addProductPrice();

        JTextArea[] fields = {productNameField, productDescriptionField,
                productManufacturerField, productQuantityField,
                productPriceField};

        for (JTextArea field : fields) {
            field.setForeground(textColor);
            field.setBackground(mainColor);
            field.setMargin(new Insets(10, 5, 10, 5));
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(secondColor);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        addProductButton(buttonPanel);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        cancelButton(buttonPanel);
        buttonPanel.add(Box.createHorizontalGlue());
        panel.add(buttonPanel);
    }

    private void addProductName() {
        productNameLabel = new JLabel("Name:");
        productNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        productNameLabel.setForeground(textColor);
        productNameLabel.setBackground(mainColor);
        productNameLabel.setFont(new Font("Helvetica", Font.BOLD, 16));
        productNameLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        panel.add(productNameLabel);

        productNameField = new JTextArea(2, 20);
        productNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, productNameField.getPreferredSize().height));
        productNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        productNameField.setFont(new Font("Helvetica", Font.ITALIC, 16));
        panel.add(productNameField);
    }

    private void addProductDescription() {
        productDescriptionLabel = new JLabel("Description:");
        productDescriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        productDescriptionLabel.setForeground(textColor);
        productDescriptionLabel.setBackground(mainColor);
        productDescriptionLabel.setFont(new Font("Helvetica", Font.BOLD, 16));
        productDescriptionLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        panel.add(productDescriptionLabel);

        productDescriptionField = new JTextArea(7, 20);
        productDescriptionField.setMaximumSize(new Dimension(Integer.MAX_VALUE, productDescriptionField.getPreferredSize().height));
        productDescriptionField.setAlignmentX(Component.CENTER_ALIGNMENT);
        productDescriptionField.setFont(new Font("Helvetica", Font.ITALIC, 16));
        productDescriptionField.setLineWrap(true);
        productDescriptionField.setWrapStyleWord(true);
        JScrollPane descriptionScrollPane = new JScrollPane(productDescriptionField);
        descriptionScrollPane.setPreferredSize(new Dimension(500, 200));
        descriptionScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(descriptionScrollPane);
    }
    private void addProductManufacturer() {
        productManufacturerLabel = new JLabel("Manufacturer:");
        productManufacturerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        productManufacturerLabel.setForeground(textColor);
        productManufacturerLabel.setBackground(mainColor);
        productManufacturerLabel.setFont(new Font("Helvetica", Font.BOLD, 16));
        productManufacturerLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        panel.add(productManufacturerLabel);

        productManufacturerField = new JTextArea(2, 20);
        productManufacturerField.setMaximumSize(new Dimension(Integer.MAX_VALUE, productNameField.getPreferredSize().height));
        productManufacturerField.setAlignmentX(Component.CENTER_ALIGNMENT);
        productManufacturerField.setFont(new Font("Helvetica", Font.ITALIC, 16));
        panel.add(productManufacturerField);
    }

    private void addProductQuantity() {
        productQuantityLabel = new JLabel("Quantity:");
        productQuantityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        productQuantityLabel.setForeground(textColor);
        productQuantityLabel.setBackground(mainColor);
        productQuantityLabel.setFont(new Font("Helvetica", Font.BOLD, 16));
        productQuantityLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        panel.add(productQuantityLabel);

        productQuantityField = new JTextArea("0",2, 20);
        productQuantityField.setMaximumSize(new Dimension(Integer.MAX_VALUE, productNameField.getPreferredSize().height));
        productQuantityField.setAlignmentX(Component.CENTER_ALIGNMENT);
        productQuantityField.setFont(new Font("Helvetica", Font.ITALIC, 16));
        panel.add(productQuantityField);
    }
    private void addProductPrice() {
        productPriceLabel = new JLabel("Price per item:");
        productPriceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        productPriceLabel.setForeground(textColor);
        productPriceLabel.setBackground(mainColor);
        productPriceLabel.setFont(new Font("Helvetica", Font.BOLD, 16));
        productPriceLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        panel.add(productPriceLabel);

        productPriceField = new JTextArea("0.0",2, 20);
        productPriceField.setMaximumSize(new Dimension(Integer.MAX_VALUE, productNameField.getPreferredSize().height));
        productPriceField.setAlignmentX(Component.CENTER_ALIGNMENT);
        productPriceField.setFont(new Font("Helvetica", Font.ITALIC, 16));
        panel.add(productPriceField);
    }

    private void addProductButton(JPanel buttonPanel) {
        addProductButton = new JButton("Create");
        addProductButton.setForeground(textColor);
        addProductButton.setBackground(mainColor);
        addProductButton.setFont(new Font("Helvetica", Font.BOLD, 16));
        addProductButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(editProduct == null){
                    addProductToGroup();
                } else {
                    editProduct();
                }

            }
        });
        buttonPanel.add(addProductButton);
    }

    private void editProduct() {
        String productName = productNameField.getText();
        String productDescription = productDescriptionField.getText();
        String productManufacturer = productManufacturerField.getText();
        int productQuantity = 0;
        double productPrice = 0.0;
        boolean parseSuccess = true;
        try {
            productQuantity = Integer.parseInt(productQuantityField.getText());
        } catch (NumberFormatException e) {
            if (!productQuantityField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Incorrect product quantity entered! Please enter an integer!", "Error", JOptionPane.ERROR_MESSAGE);
                parseSuccess = false;
            }  else productQuantity = 0;
        }
        try {
            productPrice = Double.parseDouble(productPriceField.getText());
        } catch (NumberFormatException e) {
            if (!productPriceField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Incorrect product price entry! Enter a number!", "Error", JOptionPane.ERROR_MESSAGE);
                parseSuccess = false;
            } else productPrice = 0.0;
        }

        if (productQuantity>=0 && productPrice>=0.0 && parseSuccess && !productName.isEmpty()) {
            productName = this.editProduct.name().equals(productName) ? null : productName;
            warehouse.editProduct(new Product(editProduct.id(), productName, productManufacturer, productDescription, productPrice, productQuantity));
            frame.updateGoodsTable();
            dispose();
        } else {
            if (productQuantity < 0) {
                JOptionPane.showMessageDialog(this, "The quantity of the product cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
            }
            if (productPrice < 0.0) {
                JOptionPane.showMessageDialog(this, "The price of the product cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
            }
            if (productName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Product name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    private void cancelButton(JPanel buttonPanel) {
        cancelButton = new JButton("Cancel");
        cancelButton.setForeground(textColor);
        cancelButton.setBackground(mainColor);
        cancelButton.setFont(new Font("Helvetica", Font.BOLD, 16));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
    }
    private void addProductToGroup() {
        String productName = productNameField.getText();
        String productDescription = productDescriptionField.getText();
        String productManufacturer = productManufacturerField.getText();
        int productQuantity = 0;
        double productPrice = 0.0;
        boolean parseSuccess = true;
        try {
            productQuantity = Integer.parseInt(productQuantityField.getText());
        } catch (NumberFormatException e) {
            if (!productQuantityField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Incorrect product quantity entered! Please enter an integer!", "Error", JOptionPane.ERROR_MESSAGE);
                parseSuccess = false;
            }
        }
        try {
            productPrice = Double.parseDouble(productPriceField.getText());
        } catch (NumberFormatException e) {
            if (!productPriceField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Incorrect product price entry! Enter a number!", "Error", JOptionPane.ERROR_MESSAGE);
                parseSuccess = false;
            } else productPrice = 0.0;
        }

        if (productQuantity>=0 && productPrice>=0.0 && parseSuccess && !productName.isEmpty()) {
            Product product = new Product(0, productName, productManufacturer, productDescription, productPrice, productQuantity);
            Product created = warehouse.createProduct(product);
            if (created != null) {
                warehouse.addProductToGroup(groupAddTo, created);
            }
            frame.updateGoodsTable();
            dispose();
        } else {
            if (productQuantity < 0) {
                JOptionPane.showMessageDialog(this, "The quantity of the product cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
            }
            if (productPrice < 0.0) {
                JOptionPane.showMessageDialog(this, "The price of the product cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
            }
            if (productName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Product name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        }
    }
}