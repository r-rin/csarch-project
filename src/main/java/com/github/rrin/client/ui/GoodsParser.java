package com.github.rrin.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rrin.client.StoreClientTCP;
import com.github.rrin.dto.CommandResponse;
import com.github.rrin.dto.Group;
import com.github.rrin.dto.Product;
import com.github.rrin.util.data.DataPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoodsParser {

    static ObjectMapper mapper = new ObjectMapper();

    public static Object[][] parseGroups(List<Group> groups){
        Object[][] result = new Object[groups.size()][3];

        for(int i = 0; i < groups.size(); i++){
            result[i][0] = groups.get(i);
            result[i][1] = groups.get(i).id();
            result[i][2] = groups.get(i).description();
        }
        return result;
    }

    public static Object[][] parseGroupGoods(List<Product> products) {
        Object[][] result = new Object[products.size()][6];

        for(int i = 0; i < products.size(); i++){
            result[i][0] = products.get(i);
            result[i][1] = products.get(i).id();
            result[i][2] = products.get(i).manufacturer();
            result[i][3] = products.get(i).description();
            result[i][4] = Double.toString(products.get(i).price());
            result[i][5] = Integer.toString(products.get(i).quantity());
        }
        return result;
    }

    public static Object[][] parseAllGoods(StoreClientTCP clientTCP) {
        try {
            DataPacket<CommandResponse> groupPacket = clientTCP.getAllGroups();
            CommandResponse groupResponse = groupPacket.getBody().getData();

            List<Group> groups = Arrays.asList(mapper.readValue(groupResponse.message(), Group[].class));

            List<Object[]> rows = new ArrayList<>();

            for (Group group : groups) {
                DataPacket<CommandResponse> productPacket = clientTCP.getGroupProducts(group.id());
                CommandResponse productResponse = productPacket.getBody().getData();

                List<Product> products = Arrays.asList(mapper.readValue(productResponse.message(), Product[].class));

                for (Product product : products) {
                    Object[] row = new Object[7];
                    row[0] = group;
                    row[1] = product;
                    row[2] = product.id();
                    row[3] = product.manufacturer();
                    row[4] = product.description();
                    row[5] = Double.toString(product.price());
                    row[6] = Integer.toString(product.quantity());
                    rows.add(row);
                }
            }

            return rows.toArray(new Object[0][]);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse goods from server", e);
        }
    }

    public static Object[][] getGroupPrices(StoreClientTCP client) {

        try {
            DataPacket<CommandResponse> packet = client.getAllGroups();
            CommandResponse response = packet.getBody().getData();

            List<Group> groups = Arrays.asList(mapper.readValue(response.message(), Group[].class));

            Object[][] result = new Object[groups.size()][2];

            for(int i = 0; i < groups.size(); i++){

                DataPacket<CommandResponse> packet1 = client.getGroupProducts(groups.get(i).id());
                CommandResponse response1 = packet1.getBody().getData();

                List<Product> products = Arrays.asList(mapper.readValue(response1.message(), Product[].class));

                result[i][0] = groups.get(i);
                result[i][1] = Statistics.calculateTotalValue(products);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
