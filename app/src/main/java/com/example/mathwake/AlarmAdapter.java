package com.example.mathwake;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

import androidx.appcompat.widget.SwitchCompat;

public class AlarmAdapter extends BaseAdapter {
    public interface Listener {
        void onToggle(AlarmModel alarm, boolean enabled);
        void onEdit(AlarmModel alarm);
        void onDelete(AlarmModel alarm);
    }
    private final LayoutInflater inflater;
    private final Context context;
    private List<AlarmModel> alarms;
    private final Listener listener;

    public AlarmAdapter(Context context, List<AlarmModel> alarms, Listener listener) {
        this.context = context;
        inflater = LayoutInflater.from(context); this.alarms = alarms; this.listener = listener;
    }
    public void setAlarms(List<AlarmModel> alarms) { this.alarms = alarms; notifyDataSetChanged(); }
    @Override public int getCount() { return alarms.size(); }
    @Override public Object getItem(int position) { return alarms.get(position); }
    @Override public long getItemId(int position) { return alarms.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) convertView = inflater.inflate(R.layout.item_alarm, parent, false);
        AlarmModel alarm = alarms.get(position);
        TextView time = convertView.findViewById(R.id.textTime);
        TextView label = convertView.findViewById(R.id.textLabel);
        TextView detail = convertView.findViewById(R.id.textDetail);
        SwitchCompat enabled = convertView.findViewById(R.id.switchEnabled);
        Button edit = convertView.findViewById(R.id.buttonEdit);
        Button delete = convertView.findViewById(R.id.buttonDelete);
        time.setText(context.getString(R.string.alarm_time, alarm.getHour(), alarm.getMinute()));
        label.setText(alarm.getLabel().isEmpty() ? context.getString(R.string.alarm_default_label) : alarm.getLabel());
        String countText = context.getResources().getQuantityString(
                R.plurals.alarm_question_count, alarm.getQuestionCount(), alarm.getQuestionCount());
        detail.setText(context.getString(R.string.alarm_detail, dayText(context, alarm.getRepeatDays()), countText));
        enabled.setOnCheckedChangeListener(null);
        enabled.setChecked(alarm.isEnabled());
        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onToggle(alarm, isChecked);
            }
        });
        edit.setOnClickListener(v -> listener.onEdit(alarm));
        delete.setOnClickListener(v -> listener.onDelete(alarm));
        return convertView;
    }

    public static String dayText(Context context, String days) {
        if (days == null || days.isEmpty()) return context.getString(R.string.repeat_once);
        if (days.equals("1,2,3,4,5,6,7")) return context.getString(R.string.repeat_daily);
        if (days.equals("2,3,4,5,6")) return context.getString(R.string.repeat_weekdays);
        String result = days.replace("1", context.getString(R.string.day_sun))
                .replace("2", context.getString(R.string.day_mon))
                .replace("3", context.getString(R.string.day_tue))
                .replace("4", context.getString(R.string.day_wed))
                .replace("5", context.getString(R.string.day_thu))
                .replace("6", context.getString(R.string.day_fri))
                .replace("7", context.getString(R.string.day_sat));
        return result.replace(",", ", ");
    }
}
