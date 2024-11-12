package com.foodism.givegrub.orderHistory.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.foodism.givegrub.R;
import com.foodism.givegrub.orderHistory.adapters.Order; // Import the Order class
import java.util.List;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private List<Order> orderList;

    public OrderHistoryAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.orderTitle.setText(order.getFoodItem()+"\uD83C\uDF5C\uD83C\uDF2E");
        holder.orderFoodDonorName.setText("\uD83D\uDE4BFood Donor : " + order.getFoodDonorName());
        holder.orderVenue.setText("\uD83D\uDCCDLocation : " + order.getVenue());
        holder.orderPeopleCount.setText("\uD83D\uDE4D\uD83C\uDFFBPeople : " + order.getPeopleCount()+" serves");
        holder.orderTimestamp.setText(order.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public void updateOrderHistory(List<Order> newOrderList) {
        orderList.clear();
        orderList.addAll(newOrderList);
        notifyDataSetChanged();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {

        // Make sure to import TextView
        TextView orderTitle, orderVenue, orderPeopleCount, orderTimestamp,orderFoodDonorName;

        public OrderViewHolder(View itemView) {
            super(itemView);
            // Bind the TextViews with the correct IDs from your layout
            orderTitle = itemView.findViewById(R.id.orderTitle);
            orderFoodDonorName = itemView.findViewById(R.id.orderFoodDonorName);
            orderVenue = itemView.findViewById(R.id.orderVenue);
            orderPeopleCount = itemView.findViewById(R.id.orderPeopleCount);
            orderTimestamp = itemView.findViewById(R.id.orderTimestamp);
        }
    }
}
