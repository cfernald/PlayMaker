package data;

import com.googlecode.objectify.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Chris on 3/29/2015.
 */
@Entity
public class UserData extends DataObject{

    @Id protected String id;
    protected String email;
    protected List<UserGroup> groups;
    protected List<Invite> invites;

    private UserData() {
        groups = new ArrayList<UserGroup>();
    };

    public UserData(String email, String name, String user_id) {
        this.id = user_id;
        this.name = name;
        this.email = email;

        groups = new ArrayList<UserGroup>();
    }

    public void addGroup(GroupData group) {
        if (groups == null) {
            groups = new ArrayList<UserGroup>();
        }
        groups.add(new UserGroup(group.getName(), group.getId()));

        removeInvite(group.getId());
    }

    public void removeInvite(long groupId) {
        if (invites == null)
            return;

        List<Invite> invs = new ArrayList<Invite>();
        for (int i = 0; i < invites.size(); i++) {
            if (invites.get(i).groupId != groupId)
                invs.add(invites.get(i));
        }

        invites = invs;
    }

    public void invite(String inviter, long groupId) {
        if (invites == null)
            invites = new ArrayList<Invite>();
        else
            removeInvite(groupId);


        for (UserGroup group: groups) {
            if (group.getId() == groupId)
                return;
        }

        invites.add(new Invite(groupId, inviter, System.currentTimeMillis()));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<UserGroup> getGroups() {
        return groups;
    }

    public List<Invite> getInvites() {
        return invites;
    }

    public void addInvite(String userName, long groupId) {
        if (invites == null) {
            invites = new ArrayList<Invite>();
        }

        if (groups != null) {
            for (UserGroup group : groups) {
                if (group.getId() == groupId) {
                    return;
                }
            }
        }

        for (Invite invite: invites) {
            if (invite.getGroupId() == groupId)
                return;
        }

        invites.add(new Invite(groupId, userName, System.currentTimeMillis()));
    }

    public static class UserGroup {
        public String name;
        public long id;

        public UserGroup() {};

        public UserGroup(String name, long id) {
            this.name = name;
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class Invite {
        public String inviter;
        public long groupId;
        public long date;

        public Invite() {};

        public Invite(long groupId, String inviter, long date) {
            this.groupId = groupId;
            this.inviter = inviter;
            this.date = date;
        }

        public String getInviter() {
            return inviter;
        }

        public long getDate() {
            return date;
        }

        public long getGroupId() {
            return groupId;
        }
    }

}
