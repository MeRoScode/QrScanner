package ir.meros.qrscanner;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.ViewHolder> {

    ArrayList<Person> people;
    ArrayList<Person> filteredPeople;
    Context c;

    public PersonAdapter(ArrayList<Person> people, Context c) {
        this.people = people;
        this.filteredPeople = people;
        this.c = c;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(c).inflate(R.layout.book_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindView(filteredPeople.get(position));
    }

    @Override
    public int getItemCount() {
        return filteredPeople != null ? filteredPeople.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView seatFullName, seatType, ticketId;
        private CardView cardView;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            seatFullName = itemView.findViewById(R.id.seatFullName);
            seatType = itemView.findViewById(R.id.seatType);
            ticketId = itemView.findViewById(R.id.ticketId);
            cardView = itemView.findViewById(R.id.card_view);
        }

        public void bindView(Person person) {
            if (person.getSeatType().equals("silver")) {
                cardView.setCardBackgroundColor(Color.parseColor("#C0C0C0"));
            }
            if (person.getSeatType().equals("vip")) {
                cardView.setCardBackgroundColor(Color.parseColor("#FFC107"));

            }
            if (person.getSeatType().equals("bronze")) {
                cardView.setCardBackgroundColor(Color.parseColor("#ffffff"));

            }
            seatFullName.setText("Name : " + person.getSeatFullName());
            seatType.setText(person.getSeatType().toUpperCase());
            ticketId.setText("Ticket ID : " + person.getTicketId());

        }


    }

    public void search(String query) {
        filteredPeople = new ArrayList<>();
        for (Person person : people) {
            if (person.getSeatFullName().toLowerCase().contains(query.toLowerCase())) {
                filteredPeople.add(person);
            }
        }
        notifyDataSetChanged();
    }


    public void add(Person person) {
        filteredPeople.add(0, person);
        notifyItemInserted(0);
    }


}
