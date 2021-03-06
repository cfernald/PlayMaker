package casuals.filthy.playmaker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import casuals.filthy.playmaker.data.AsyncResponse;
import casuals.filthy.playmaker.data.DatastoreAdapter;
import casuals.filthy.playmaker.data.beans.GroupBean;
import casuals.filthy.playmaker.data.beans.GroupUserBean;
import casuals.filthy.playmaker.data.beans.UserBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shane on 3/27/2015.
 */
public class UserActivity extends BaseActivity implements AsyncResponse{

    private String userId;
    private String userName;
    private String userEmail;
    private List<Long> groupIds;
    private ProgressDialog progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            userId = extras.getString("ID");
            userEmail = extras.getString("EMAIL");
            userName = extras.getString("DISPLAY_NAME");
        }
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Retrieving your data...");
        progress.show();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.user);
        final TextView user_name = (TextView) findViewById(R.id.user_display);
        final Button createGroup = (Button) findViewById(R.id.groupCreate);
        ImageView previewThumbnail = (ImageView) findViewById(R.id.user_image);
        Bitmap b = BaseActivity.personImageView;
        previewThumbnail.setImageBitmap(b);

        TabHost host = (TabHost)findViewById(R.id.user_tabHost);
        host.setup();

        host.addTab(host.newTabSpec("one")
                .setIndicator("Your Groups")
                .setContent(new TabHost.TabContentFactory() {

                    public View createTabContent(String tag) {
                        return findViewById(R.id.user_group_list);
                    }
                }));

        host.addTab(host.newTabSpec("two")
                .setIndicator("Your Invites")
                .setContent(new TabHost.TabContentFactory() {

                    public View createTabContent(String tag) {
                        return findViewById(R.id.user_invites);
                    }
                }));

        user_name.setText(userName);
        createGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                final EditText input = new EditText(v.getContext());
                input.setImeOptions(EditorInfo.IME_ACTION_DONE);
                input.setSingleLine();
                alert.setTitle("Enter a name for your group.");
                alert.setView(input);
                alert.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (input.getText().toString().matches("")) {
                            Toast.makeText(getApplicationContext(), "You did not enter anything", Toast.LENGTH_SHORT).show();
                        } else if (input.getText().toString().length() < 3) {
                            Toast.makeText(getApplicationContext(), "Group name must be at least 3 letters long", Toast.LENGTH_SHORT).show();
                        } else {
                            String temp = input.getText().toString();
                            DatastoreAdapter adapter = new DatastoreAdapter(UserActivity.this);
                            adapter.createGroup(temp, userId);

                        }
                    }
                });
                alert.show();
            }
        });

        DatastoreAdapter adapter = new DatastoreAdapter(this);
        adapter.getUser(userId, userName, userEmail);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        BaseActivity.login=0;
    }

    @Override
    public void signOut() {
        super.signOut();
    }

    public static Button userSignOut;
    public void checkforSignOut(View v){
                userSignOut = (Button) findViewById(R.id.signOut);
                        BaseActivity.login=3;
                        userSignOut = null;
                        signOut();
                        Intent i = new Intent(getApplicationContext(), BaseActivity.class);
                        startActivity(i);
                        finish();

    }



    @Override
       public void response(Object o) {
           if(!(o instanceof UserBean)) {
               return;
           }
           UserBean user = (UserBean) o;

           List<UserBean.UserGroupBean> groupList = user.getGroups();
           List<UserBean.Invite> inviteList = user.getInvites();

           ListView listView = (ListView) findViewById(R.id.user_group_list);
           ListView listView2 = (ListView) findViewById(R.id.user_invites);

           //   For Groups
           List<String> list = new ArrayList<String>();
           List<Long> idList = new ArrayList<Long>();
           for(UserBean.UserGroupBean group: groupList) {
               list.add(group.getName());
               idList.add(group.getId());
           }

           //   For Invites
           long[] inviteIds = new long[inviteList.size()];
           String[] list2 = new String[inviteList.size()];
           for(int i = 0; i < inviteList.size(); i++) {
               list2[i] = ("Invite from " + inviteList.get(i).getInviter());
               inviteIds[i] = inviteList.get(i).getGroupId();
           }

           setGroupIds(idList);
           ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1, list);
           listView.setAdapter(arrayAdapter);
           listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
               @Override
               public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                   Long groupId = getGroupIds().get(position);

                   Intent i = new Intent(getApplicationContext(), GroupActivity.class);
                   i.putExtra("USER_ID", getId());
                   i.putExtra("GROUP_ID", groupId);
                   i.putExtra("USER_NAME", userName);
                   startActivity(i);
               }
           });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
                {
                    final Long groupId = getGroupIds().get(position);
                    AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
                    final AlertDialog dialog = alert.create();
                    dialog.setTitle("Options");
                    ListView opt = new ListView(view.getContext());
                    List<String> optionHeadings = new ArrayList<String>();
                    optionHeadings.add("Open");
                    optionHeadings.add("Delete");
                    ListAdapter optionsAdapter = new ArrayAdapter<String>(getBaseContext(), R.layout.user_group_options,R.id.user_option ,optionHeadings);
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
                                Intent i = new Intent(getApplicationContext(), GroupActivity.class);
                                i.putExtra("USER_ID", getId());
                                i.putExtra("GROUP_ID", groupId);
                                i.putExtra("USER_NAME", userName);
                                startActivity(i);
                                dialog.dismiss();
                            }
                            else if(position ==1){
                                new DatastoreAdapter(UserActivity.this).leaveGroup(groupId,getId());
                                dialog.dismiss();
                            }
                        }
                    });



                    dialog.show();
                    return true;
                }

        });


           InviteAdapter inviteAdapter = new InviteAdapter(this , R.layout.invitation, R.id.invite_text, list2, inviteIds, this, userId);
           listView2.setAdapter(inviteAdapter);

           progress.dismiss();
       }


    public String getName(){
        return userName;
    }
    public String getEmail(){
        return userEmail;
    }
    public String getId(){
        return userId;
    }
    public List<Long> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Long> list) {
        groupIds = list;
    }

    private class InviteAdapter extends ArrayAdapter<String> {

        private final String userId;
        private long[] ids;
        private AsyncResponse resp;

        public InviteAdapter(Context context, int resource, int textView, String[] objects, long[] ids, AsyncResponse resp, String userId) {
            super(context, resource, textView, objects);

            this.ids = ids;
            this.resp = resp;
            this.userId = userId;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            Button accept = (Button) v.findViewById(R.id.invite_accept);
            Button ignore = (Button) v.findViewById(R.id.invite_ignore);

            accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new DatastoreAdapter(resp).joinGroup(ids[position], userId);
                }
            });

            ignore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new DatastoreAdapter(resp).inviteRemove(ids[position], userId);
                }
            });

            return v;

        }

    }
}
