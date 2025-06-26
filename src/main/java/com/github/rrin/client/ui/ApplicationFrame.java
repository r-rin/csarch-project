package com.github.rrin.client.ui;

import com.github.rrin.client.StoreClientTCP;
import com.github.rrin.dto.Group;
import com.github.rrin.dto.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ApplicationFrame extends JFrame implements ActionListener {

    Color textColor = new Color(0x2F4052);
    Color mainColor = new Color(0xD8E1E9);
    Color secondColor = new Color(0xE7EBF1);
    Color thirdColor = new Color(0x759EB8);
    Color tableBackground = new Color(0xE7EAF3);

    StoreClientTCP client;
    Warehouse warehouse;
    Group choosedGroup = null;
    Product choosedFoundProduct;

    JFrame mainFrame = new JFrame();
    JPanel contentPanel = new JPanel(new GridLayout(1, 1));

    JButton mainPage;
    JButton goodsPage;
    JButton statisticsPage;

    JTable goodsTable = new JTable();
    JTable groupTable = new JTable();
    JTable foundProductTable = new JTable();
    JTable groupPricesTable = new JTable();
    JTable allPoductsTable = new JTable();

    String[] columnNames = {"Title", "ID", "Description"};
    String[] goodsColumnNames = {"Title", "ID", "Manufacturer", "Description", "Price", "Quantity"};

    JPanel goodsTablePanel;
    JPanel findProductTablePanel;
    JPanel groupTablePanel;

    JPanel productInfoPanel;

    JPanel groupPricesTablePanel;
    JPanel allProductsTablePanel;

    //Goods page buttons
    JButton addGroup;
    JButton removeGroup;
    JButton editGroup;

    JButton addGoods;
    JButton removeGoods;
    JButton editGoods;

    JButton increaseGoods;
    JButton decreaseGoods;

    JButton findProducts;

    JLabel foundProductName = new JLabel();
    JLabel foundProductAuthor = new JLabel();
    JTextArea foundProductDescription = new JTextArea();
    JLabel foundProductPublisher = new JLabel();
    JLabel foundProductQuanity = new JLabel();
    JLabel foundProductPrice = new JLabel();

    JTextField nameField;
    JTextField authorField;

    public ApplicationFrame(String host, int port) throws UnsupportedLookAndFeelException {
        this.client = new StoreClientTCP(host, port);
        this.client.start();
        this.warehouse = new Warehouse(client);

        mainFrame.setTitle("Rusty Warehouse");
        mainFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        mainFrame.setMinimumSize(new Dimension(1000, 500));
        mainFrame.setLayout(new BorderLayout());

        initMenuBar();
        initMainPageContent();

        mainFrame.add(contentPanel, BorderLayout.CENTER);

        mainFrame.setVisible(true);
    }

    private void clearContentPanel() {
        contentPanel.removeAll();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(thirdColor);
        menuBar.setBorderPainted(false);
        menuBar.setLayout(new FlowLayout(FlowLayout.CENTER));

        //Creating menu buttons
        mainPage = new JButton("Info");
        mainPage.setFocusable(false);
        mainPage.setBackground(mainColor);
        mainPage.setForeground(textColor);

        goodsPage = new JButton("Goods");
        goodsPage.setFocusable(false);
        goodsPage.setBackground(mainColor);
        goodsPage.setForeground(textColor);

        statisticsPage = new JButton("Statistics");
        statisticsPage.setFocusable(false);
        statisticsPage.setBackground(mainColor);
        statisticsPage.setForeground(textColor);

        menuBar.add(mainPage);
        menuBar.add(goodsPage);
        menuBar.add(statisticsPage);
        
        mainPage.addActionListener(this);
        goodsPage.addActionListener(this);
        statisticsPage.addActionListener(this);

        mainFrame.add(menuBar, BorderLayout.NORTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == mainPage){
            clearContentPanel();
            initMainPageContent();
        } else if(e.getSource() == goodsPage) {
            clearContentPanel();
            initGoodsPage();
        } else if (e.getSource() == statisticsPage) {
            clearContentPanel();
            initStatisticsPage();
        } else if (e.getSource() == addGroup){
            AddGroupUI addGroupDialog = new AddGroupUI(this, warehouse);
            addGroupDialog.setVisible(true);
        } else if (e.getSource() == removeGroup){
            try {
                Group choosedGroup = (Group) groupTable.getValueAt(groupTable.getSelectedRow(), 0);
                int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the group " + choosedGroup.name() + "?", "Delete group", JOptionPane.YES_NO_OPTION);
                if(choice == JOptionPane.YES_OPTION){
                    warehouse.deleteProductsGroup(choosedGroup);
                    updateGroupTable();
                    this.choosedGroup = null;
                    JOptionPane.showMessageDialog(null, "Group " + choosedGroup.name() + " has been deleted");
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(null, "First, select a group!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == editGroup){
            try {
                Group group = (Group) groupTable.getValueAt(groupTable.getSelectedRow(), 0);
                AddGroupUI editGroupUI = new AddGroupUI(group,this, warehouse);
                editGroupUI.setVisible(true);
            } catch (ArrayIndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(null, "First, select a group!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == addGoods){
            if (choosedGroup!=null){
                AddProductToGroupUI addProductDialog = new AddProductToGroupUI(choosedGroup, warehouse, this);
                addProductDialog.setVisible(true);
            } else{
                JOptionPane.showMessageDialog(null, "First, select a group!", "Error", JOptionPane.ERROR_MESSAGE);

            }

        } else if (e.getSource() == removeGoods){
            try {
                Product product = (Product) goodsTable.getValueAt(goodsTable.getSelectedRow(), 0);
                int choice = JOptionPane.showConfirmDialog(null, "You are sure you want to delete the product " + product.name() + "?", "Deleting", JOptionPane.YES_NO_OPTION);
                if(choice == JOptionPane.YES_OPTION){
                    warehouse.deleteProduct(product);
                    updateGoodsTable();
                }

            } catch (ArrayIndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(null, "First, choose a product!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == editGoods){
            try {
                Product product = (Product) goodsTable.getValueAt(goodsTable.getSelectedRow(), 0);
                AddProductToGroupUI addProductToGroupUI = new AddProductToGroupUI(product, choosedGroup,  warehouse, this);
                addProductToGroupUI.setVisible(true);
            } catch (ArrayIndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(null, "First, choose a product!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == increaseGoods){
            try {
                Product product = (Product) goodsTable.getValueAt(goodsTable.getSelectedRow(), 0);
                int oldQuanity = product.quantity();

                warehouse.editProductQuantity(product, oldQuanity+1);
                updateGoodsTable();
            } catch (ArrayIndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(null, "First, choose a product!", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == decreaseGoods){
            try {
                Product product = (Product) goodsTable.getValueAt(goodsTable.getSelectedRow(), 0);
                int oldQuanity = product.quantity();

                if(oldQuanity <= 0){
                    JOptionPane.showMessageDialog(null, "The quantity of goods cannot be less than 0", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    warehouse.editProductQuantity(product, oldQuanity-1);
                    updateGoodsTable();
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                JOptionPane.showMessageDialog(null, "First, choose a product!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void initProductInfoPanel(Product product) {
        if(product != null){
            foundProductName.setText("Name: "+product.name());
            foundProductName.setForeground(textColor);
            foundProductName.setBackground(secondColor);
            foundProductAuthor.setText("Manufacturer: "+product.manufacturer());
            foundProductAuthor.setForeground(textColor);
            foundProductAuthor.setBackground(secondColor);
            foundProductDescription = new JTextArea(product.description());
            foundProductQuanity.setText("Quantity in storage: "+product.quantity());
            foundProductQuanity.setForeground(textColor);
            foundProductQuanity.setBackground(secondColor);
            foundProductPrice.setText("Price per item: "+ product.price());
            foundProductPrice.setForeground(textColor);
            foundProductPrice.setBackground(secondColor);

            foundProductDescription.setEditable(false);
            foundProductDescription.setLineWrap(true);
            foundProductDescription.setOpaque(true);
            foundProductDescription.setForeground(textColor);
            foundProductDescription.setBackground(secondColor);

            JScrollPane scrollPane = new JScrollPane(foundProductDescription);
            scrollPane.getViewport().setBackground(mainColor);

            productInfoPanel.add(foundProductName);
            productInfoPanel.add(foundProductAuthor);

            productInfoPanel.add(foundProductPublisher);
            productInfoPanel.add(foundProductQuanity);
            productInfoPanel.add(foundProductPrice);
            JLabel desclabel = new JLabel("Description");
            productInfoPanel.add(desclabel);
            productInfoPanel.add(scrollPane);
        }
    }

    private void initFoundProductTable(Object[][] array) {
        foundProductTable = new JTable(array, new String[]{"Product"});
        foundProductTable.setShowGrid(true);
        foundProductTable.setGridColor(Color.BLACK);
        foundProductTable.setBackground(tableBackground);
        foundProductTable.setOpaque(false);
        foundProductTable.getTableHeader().setBackground(mainColor);
        foundProductTable.getTableHeader().setForeground(textColor);

        foundProductTable.getSelectionModel().addListSelectionListener(e -> {
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            if ( !e.getValueIsAdjusting() && !lsm.isSelectionEmpty()) {
                choosedFoundProduct = (Product) foundProductTable.getValueAt(foundProductTable.getSelectedRow(), 0);
                productInfoPanel.removeAll();
                productInfoPanel.revalidate();
                productInfoPanel.repaint();
                initProductInfoPanel(choosedFoundProduct);
            }
        });

        for(int i = 0; i < foundProductTable.getColumnCount(); i++){
            foundProductTable.getColumnModel().getColumn(i).setCellRenderer(new MyCellRenderer());
        }

        JScrollPane scrollPane = new JScrollPane(foundProductTable);
        scrollPane.getViewport().setBackground(tableBackground);
        findProductTablePanel.add(scrollPane);
    }

    protected void updateGroupTable() {
        groupTablePanel.removeAll();
        groupTablePanel.revalidate();
        groupTablePanel.repaint();

        initGroupTable(warehouse);
        groupTable.getSelectionModel().addListSelectionListener(e -> {
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            if ( !e.getValueIsAdjusting() && !lsm.isSelectionEmpty()) {
                choosedGroup = (Group) groupTable.getValueAt(groupTable.getSelectedRow(), 0);
                updateGoodsTable();
            }
        });
        goodsTablePanel.removeAll();
        goodsTablePanel.revalidate();
        goodsTablePanel.repaint();
    }

    protected void updateGoodsTable() {
        Group group;
        try {
            group = (Group) groupTable.getValueAt(groupTable.getSelectedRow(), 0);
        } catch (Exception exception){
            group = choosedGroup;
        }

        goodsTablePanel.removeAll();
        goodsTablePanel.revalidate();
        goodsTablePanel.repaint();

        goodsTablePanel.setBackground(secondColor);

        List<Product> products = warehouse.getGroupProducts(group.id());

        goodsTable.removeAll();
        goodsTable = new JTable(GoodsParser.parseGroupGoods(products), goodsColumnNames);
        goodsTable.setBackground(tableBackground);
        goodsTable.getTableHeader().setBackground(mainColor);
        goodsTable.getTableHeader().setForeground(textColor);
        MyCellRenderer cellRenderer = new MyCellRenderer();
        for(int i = 0; i < goodsTable.getColumnCount(); i++){
            goodsTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        goodsTable.getColumnModel().getColumn(2).setMinWidth(200);
        goodsTable.getColumnModel().getColumn(3).setWidth(50);
        goodsTable.getColumnModel().getColumn(4).setWidth(50);

        goodsTable.setShowGrid(true);
        goodsTable.setGridColor(Color.BLACK);

        JScrollPane goodsTableScrollLambda = new JScrollPane(goodsTable);
        goodsTableScrollLambda.getViewport().setBackground(tableBackground);
        goodsTablePanel.add(goodsTableScrollLambda);
    }

    private void initMainPageContent() {
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.PAGE_AXIS));
        mainContentPanel.setBackground(secondColor);

        JLabel welcomeText = new JLabel("Welcome to the Rusty Warehouse!");
        welcomeText.setBorder(new EmptyBorder(135, 0,10, 0));
        welcomeText.setForeground(textColor);
        welcomeText.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeText.setFont(new Font("Helvetica", Font.BOLD, 20));
        mainContentPanel.add(welcomeText);

        JLabel instructionsText = new JLabel("To change the page, click one of the buttons above.");
        instructionsText.setForeground(textColor);
        instructionsText.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionsText.setFont(new Font("Helvetica", Font.ITALIC, 16));
        mainContentPanel.add(instructionsText);

        JLabel mainText = new JLabel("Info - returns you to this page");
        mainText.setForeground(textColor);
        mainText.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainText.setFont(new Font("Helvetica", Font.PLAIN, 16));
        mainContentPanel.add(mainText);

        JLabel goodsText = new JLabel("Goods - a page for managing groups and their products");
        goodsText.setForeground(textColor);
        goodsText.setAlignmentX(Component.CENTER_ALIGNMENT);
        goodsText.setFont(new Font("Helvetica", Font.PLAIN, 16));
        mainContentPanel.add(goodsText);

        JLabel statisticsText = new JLabel("Statistics - page with statistical data");
        statisticsText.setForeground(textColor);
        statisticsText.setAlignmentX(Component.CENTER_ALIGNMENT);
        statisticsText.setFont(new Font("Helvetica", Font.PLAIN, 16));
        mainContentPanel.add(statisticsText);

        contentPanel.add(mainContentPanel);
    }

    private void initStatisticsPage() {
        JPanel statiscticsContentPanel = new JPanel();
        statiscticsContentPanel.setBackground(secondColor);
        statiscticsContentPanel.setLayout(new BorderLayout());

        JPanel statiscticsDataPanel = new JPanel();
        statiscticsDataPanel.setBackground(secondColor);

        List<Product> allProducts = warehouse.getAllProducts();

        JLabel totalValueLabel = new JLabel("Total price: "+
                Math.round(Statistics.calculateTotalValue(allProducts)*100000.0)/100000.0
                +" uah.");

        totalValueLabel.setBackground(secondColor);
        totalValueLabel.setForeground(textColor);

        groupPricesTablePanel = new JPanel();
        groupPricesTablePanel.setBackground(secondColor);
        groupPricesTablePanel.setLayout(new GridLayout(1, 1));
        groupPricesTablePanel.setPreferredSize(new Dimension(250, Integer.MAX_VALUE));

        initGroupPricesTable();

        groupPricesTablePanel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));

        allProductsTablePanel = new JPanel();
        allProductsTablePanel.setBackground(secondColor);

        allProductsTablePanel.setLayout(new GridLayout(1, 1));

        initAllProductsTable();

        statiscticsDataPanel.add(totalValueLabel);
        statiscticsContentPanel.add(statiscticsDataPanel, BorderLayout.NORTH);
        statiscticsContentPanel.add(groupPricesTablePanel, BorderLayout.WEST);
        statiscticsContentPanel.add(allProductsTablePanel, BorderLayout.CENTER);
        contentPanel.add(statiscticsContentPanel);
    }

    private void initAllProductsTable() {
        allPoductsTable = new JTable(GoodsParser.parseAllGoods(client), new String[]{"Group", "Name", "ID", "Manufacturer", "Description", "Price", "Quantity"});
        allPoductsTable.setBackground(tableBackground);
        allPoductsTable.setForeground(textColor);
        allPoductsTable.getTableHeader().setForeground(textColor);
        allPoductsTable.getTableHeader().setBackground(mainColor);

        for(int i = 0; i < allPoductsTable.getColumnCount(); i++) {
            allPoductsTable.getColumnModel().getColumn(i).setCellRenderer(new MyCellRenderer());
        }
        allPoductsTable.getColumnModel().getColumn(5).setMaxWidth(75);
        allPoductsTable.getColumnModel().getColumn(6).setMaxWidth(75);
        
        JScrollPane jScrollPane = new JScrollPane(allPoductsTable);
        jScrollPane.getViewport().setBackground(tableBackground);
        allProductsTablePanel.add(jScrollPane);
    }

    private void initGroupPricesTable() {
        groupPricesTable = new JTable(GoodsParser.getGroupPrices(client), new String[]{"Name", "Total"});
        groupPricesTable.setBackground(tableBackground);
        groupPricesTable.setForeground(textColor);
        groupPricesTable.getTableHeader().setForeground(textColor);
        groupPricesTable.getTableHeader().setBackground(mainColor);

        for(int i = 0; i < groupPricesTable.getColumnCount(); i++){
            groupPricesTable.getColumnModel().getColumn(i).setCellRenderer(new MyCellRenderer());
        }
        JScrollPane jScrollPane = new JScrollPane(groupPricesTable);
        jScrollPane.getViewport().setBackground(tableBackground);
        groupPricesTablePanel.add(jScrollPane);
    }

    private void initGoodsPage() {

        JPanel goodsContentPanel = new JPanel();
        goodsContentPanel.setBackground(secondColor);
        goodsContentPanel.setLayout(new BorderLayout());

        //ЛІВА ЧАСТИНА ВІКНА

        JPanel groupPanel = new JPanel();
        groupPanel.setBackground(secondColor);
        groupPanel.setPreferredSize(new Dimension(400, 0));
        groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));

        groupTablePanel = new JPanel(new GridLayout(1, 1));
        groupTablePanel.setBackground(secondColor);
        groupTablePanel.setBorder(new EmptyBorder(5,5,5,5));

        initGroupTable(warehouse);

        JPanel groupButtonsPanel = new JPanel();
        groupButtonsPanel.setBackground(secondColor);
        groupButtonsPanel.setLayout(new BoxLayout(groupButtonsPanel, BoxLayout.X_AXIS));

        addGroup = new JButton("Add");
        addGroup.setBackground(mainColor);
        addGroup.setFocusable(false);
        addGroup.setForeground(textColor);
        removeGroup = new JButton("Delete");
        removeGroup.setBackground(mainColor);
        removeGroup.setFocusable(false);
        removeGroup.setForeground(textColor);
        editGroup = new JButton("Edit");
        editGroup.setBackground(mainColor);
        editGroup.setFocusable(false);
        editGroup.setForeground(textColor);

        addGroup.addActionListener(this);
        removeGroup.addActionListener(this);
        editGroup.addActionListener(this);

        groupButtonsPanel.add(addGroup);
        groupButtonsPanel.add(removeGroup);
        groupButtonsPanel.add(editGroup);

        groupPanel.add(groupTablePanel);
        groupPanel.add(groupButtonsPanel);

        //ЦЕНТРАЛЬНА ЧАСТИНА ВІКНА

        JPanel goodsPanel = new JPanel();
        goodsPanel.setBackground(secondColor);
        goodsPanel.setLayout(new BoxLayout(goodsPanel, BoxLayout.Y_AXIS));

        goodsTablePanel = new JPanel(new GridLayout(1, 1));
        goodsPanel.setBackground(secondColor);
        goodsTablePanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        if(choosedGroup == null){
            goodsTable = new JTable();
            goodsTable.setBackground(tableBackground);
            goodsTable.getTableHeader().setBackground(mainColor);
            goodsTable.getTableHeader().setForeground(textColor);

            JScrollPane goodsTableScroll = new JScrollPane(goodsTable);
            goodsTableScroll.getViewport().setBackground(tableBackground);
            goodsTablePanel.add(goodsTableScroll);
        } else {
            updateGoodsTable();
        }

        groupTable.getSelectionModel().addListSelectionListener(e -> {
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            if ( !e.getValueIsAdjusting() && !lsm.isSelectionEmpty()) {
                choosedGroup = (Group) groupTable.getValueAt(groupTable.getSelectedRow(), 0);
                updateGoodsTable();
            }
        });

        JPanel goodsButtonsPanel = new JPanel();
        goodsButtonsPanel.setLayout(new BoxLayout(goodsButtonsPanel, BoxLayout.X_AXIS));

        increaseGoods = new JButton("+");
        increaseGoods.setBackground(mainColor);
        increaseGoods.setFocusable(false);
        increaseGoods.setForeground(textColor);
        decreaseGoods = new JButton("-");
        decreaseGoods.setBackground(mainColor);
        decreaseGoods.setFocusable(false);
        decreaseGoods.setForeground(textColor);

        addGoods = new JButton("Add");
        addGoods.setBackground(mainColor);
        addGoods.setFocusable(false);
        addGoods.setForeground(textColor);
        removeGoods = new JButton("Delete");
        removeGoods.setBackground(mainColor);
        removeGoods.setFocusable(false);
        removeGoods.setForeground(textColor);
        editGoods = new JButton("Edit");
        editGoods.setBackground(mainColor);
        editGoods.setFocusable(false);
        editGoods.setForeground(textColor);

        increaseGoods.addActionListener(this);
        decreaseGoods.addActionListener(this);
        addGoods.addActionListener(this);
        removeGoods.addActionListener(this);
        editGoods.addActionListener(this);

        goodsButtonsPanel.add(increaseGoods);
        goodsButtonsPanel.add(decreaseGoods);

        goodsButtonsPanel.add(addGoods);
        goodsButtonsPanel.add(removeGoods);
        goodsButtonsPanel.add(editGoods);

        goodsPanel.add(goodsTablePanel);
        goodsPanel.add(goodsButtonsPanel);

        goodsContentPanel.add(groupPanel, BorderLayout.WEST);
        goodsContentPanel.add(goodsPanel, BorderLayout.CENTER);

        contentPanel.add(goodsContentPanel);
    }

    private void initGroupTable(Warehouse warehouse) {

        List<Group> groups = warehouse.getGroups();

        groupTable = new JTable(GoodsParser.parseGroups(groups), columnNames);
        groupTable.getColumnModel().getColumn(0).setCellRenderer(new MyCellRenderer());
        groupTable.getColumnModel().getColumn(1).setCellRenderer(new MyCellRenderer());
        groupTable.setShowGrid(true);
        groupTable.setGridColor(Color.BLACK);
        groupTable.setForeground(textColor);
        groupTable.setBackground(tableBackground);
        groupTable.setOpaque(true);
        groupTable.getTableHeader().setForeground(textColor);
        groupTable.getTableHeader().setBackground(mainColor);

        JScrollPane scrollPaneTable = new JScrollPane(groupTable);
        scrollPaneTable.getViewport().setBackground(tableBackground);
        groupTablePanel.add(scrollPaneTable);
    }
}
