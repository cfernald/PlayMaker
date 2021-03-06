package casuals.filthy.playmaker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import casuals.filthy.playmaker.data.AsyncResponse;
import casuals.filthy.playmaker.data.DatastoreAdapter;
import casuals.filthy.playmaker.data.beans.GroupBean;

/**
 * Created by Steven on 4/25/2015.
 */
public class EventFragment extends Fragment implements AsyncResponse{

    private GroupBean group;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View eventView = inflater.inflate(R.layout.events_tab, container,false);
        return eventView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button eventCreate = (Button) getView().findViewById(R.id.eventCreate);
        eventCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(), EventCreate.class);
                List<String> eventTypes = group.getEventTypes();
                int index = 0;
                for(String type: eventTypes) {
                    i.putExtra("EVENT_TYPE_" + index, type);
                    index++;
                }
                startActivity(i);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }

    @Override
    public void response(Object o) {
        if(!(o instanceof GroupBean)) {
            return;
        }
        group = (GroupBean) o;
        refreshView();
    }

    private void refreshView() {

        if (group == null)
            return;
        List<GroupBean.GroupEventData> eventList = group.getEventsUpcoming();
        if (getView() == null)
            return;
        ListView listView = (ListView) getView().findViewById(R.id.group_event_list);
        TextView name = (TextView) getView().findViewById(R.id.group_name);
        name.setText(group.getName());
        List<String> list = new ArrayList<String>();
        List<Long> idList = new ArrayList<Long>();
        for(GroupBean.GroupEventData event: eventList) {
            list.add(event.getName());
            idList.add(event.getEventId());
        }

        if (list.size() == 0) {
            (getView().findViewById(R.id.np_events_text)).setVisibility(View.VISIBLE);
        } else {
            (getView().findViewById(R.id.np_events_text)).setVisibility(View.GONE);
        }

        GroupActivity.setEventIds(idList);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_expandable_list_item_1, list);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Long eventId = GroupActivity.getEventIds().get(position);
                group.getEventTypes();
                Intent i = new Intent(getActivity().getApplicationContext(), EventActivity.class);
                i.putExtra("EVENT_ID", eventId);
                startActivity(i);
            }
        });


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                final Long eventId = GroupActivity.getEventIds().get(position);
                AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
                final AlertDialog dialog = alert.create();
                dialog.setTitle("Options");
                ListView opt = new ListView(view.getContext());
                List<String> optionHeadings = new ArrayList<String>();
                optionHeadings.add("Open");
                if(GroupActivity.isAdmin()){
                optionHeadings.add("Delete");
                }
                ListAdapter optionsAdapter = new ArrayAdapter<String>(getActivity().getBaseContext(), R.layout.user_group_options,R.id.user_option ,optionHeadings);
                opt.setAdapter(optionsAdapter);
                dialog.setView(opt);
                dialog.setButton("Done",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                opt.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if(position == 0)
                        {
                            Intent i = new Intent(getActivity().getApplicationContext(), EventActivity.class);
                            i.putExtra("EVENT_ID", eventId);
                            startActivity(i);
                            dialog.dismiss();
                        }
                        else if(position ==1){
                            new DatastoreAdapter(EventFragment.this).leaveEvent(eventId,GroupActivity.getUserId());
                            dialog.dismiss();
                        }
                    }
                });



                dialog.show();
                return true;
            }

        });


    }
}