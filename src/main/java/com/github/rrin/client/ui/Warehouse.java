package com.github.rrin.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rrin.client.StoreClientTCP;
import com.github.rrin.dto.CommandResponse;
import com.github.rrin.dto.Group;
import com.github.rrin.dto.Product;
import com.github.rrin.util.data.DataPacket;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Warehouse {

    ObjectMapper objectMapper = new ObjectMapper();
    private final StoreClientTCP client;

    public Warehouse(StoreClientTCP client) {
        this.client = client;
    }

    public Product createProduct(Product product) {
        try {
            DataPacket<CommandResponse> packet = client.createProduct(
                    product.name(),
                    product.manufacturer(),
                    product.description(),
                    product.price(),
                    product.quantity()
            );
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 201) {
                Product prod = objectMapper.readValue(response.message(), Product.class);
                JOptionPane.showMessageDialog(null, "Product created successfully: " + prod.toFormatted(), "Success", JOptionPane.INFORMATION_MESSAGE);
                return prod;
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void editGroup(Group group) {
        try {
            DataPacket<CommandResponse> packet = client.updateGroup(group.id(), group.name(), group.description());
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                JOptionPane.showMessageDialog(null, "Group updated successfully: " + group.toFormatted(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addProductGroup(String name, String description) throws Exception {
        if (name == null || name.isEmpty()) {
            throw new Exception("Group name cannot be empty");
        }

        if (description == null || description.isEmpty()) {
            description = "No description provided";
        }

        DataPacket<CommandResponse> packet = client.createGroup(name, description);
        CommandResponse response = packet.getBody().getData();

        if (response.statusCode() == 201) {
            DataPacket<CommandResponse> newGroupPacket = client.getGroup(objectMapper.readValue(response.message(), Integer.class));
            Group newGroup = objectMapper.readValue(newGroupPacket.getBody().getData().message(), Group.class);
            JOptionPane.showMessageDialog(null, "Group has been created: " + newGroup.toFormatted(), "Success!", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);

        }
    }

    public void deleteProductsGroup(Group group) {
        try {
            ArrayList<Product> toDelete = getGroupProducts(group.id());
            DataPacket<CommandResponse> packet = client.deleteGroup(group.id());
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                JOptionPane.showMessageDialog(null, "Group has been deleted: " + group.toFormatted(), "Success!", JOptionPane.INFORMATION_MESSAGE);

                for (Product p : toDelete) {
                    deleteProduct(p);
                }
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addProductToGroup(Group group, Product product) {
        try {
            DataPacket<CommandResponse> packet = client.addProductToGroup(product.id(), group.id());
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                JOptionPane.showMessageDialog(null,response.message(), response.title(), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void editProductQuantity(Product product, int newQuantity) {
        try {
            DataPacket<CommandResponse> packet = client.updateProduct(product.id(), null, null, null, null, newQuantity);
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                JOptionPane.showMessageDialog(null,response.message(), response.title(), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteProduct(Product product) {
        try {
            DataPacket<CommandResponse> packet = client.deleteProduct(product.id());
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                JOptionPane.showMessageDialog(null,response.message(), response.title(), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Group> getGroups() {
        try {
            DataPacket<CommandResponse> packet = client.getAllGroups();
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                return new ArrayList<>(List.of(objectMapper.readValue(response.message(), Group[].class)));
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>();
    }

    public ArrayList<Product> getAllProducts() {
        try {
            DataPacket<CommandResponse> packet = client.getAllProducts();
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                return new ArrayList<>(List.of(objectMapper.readValue(response.message(), Product[].class)));
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>();
    }

    public ArrayList<Product> getGroupProducts(int groupId) {
        try {
            DataPacket<CommandResponse> packet = client.getGroupProducts(groupId);
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                return new ArrayList<>(List.of(objectMapper.readValue(response.message(), Product[].class)));
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>();
    }

    public void editProduct(Product product) {
        try {
            DataPacket<CommandResponse> packet = client.updateProduct(product.id(), product.name(), product.manufacturer(), product.description(), product.price(), product.quantity());
            CommandResponse response = packet.getBody().getData();

            if (response.statusCode() == 200) {
                JOptionPane.showMessageDialog(null,response.message(), response.title(), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, response.message(), response.title(), JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



