package casuals.filthy.playmaker.backend;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import data.EventData;
import data.GroupData;
import data.GroupUserDetailed;
import data.UserData;

import static casuals.filthy.playmaker.backend.OfyService.ofy;

/**
 * Created by Chris on 3/27/2015.
 */
public class GroupsServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(GroupsServlet.class.getName());
    private static Gson gson = new Gson();

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> params = ServletUtils.getParams(req.getInputStream());

        // get the parameters
        String userId = (String) params.get("user_id");//req.getParameter("user_id");
        String groupName = (String) params.get("group_name");//req.getParameter("group_name");
        String action = (String) params.get("action");

        if (userId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing user_id or group_name");
            return;
        }

        // get the User data
        UserData user = ofy().load().type(UserData.class).id(userId).now();
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "user not found");
            return;
        }

        GroupData group;

        if (action != null && action.equals("notify")) {
            String groupIdString = (String) params.get("group_id");
            String userName = (String) params.get("user_name");
            String message = (String) params.get("message");
            if (groupIdString == null || userName == null || message == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing fields");
                return;
            }
            long groupId = Long.parseLong(groupIdString);

            group = ofy().load().type(GroupData.class).id(groupId).now();
            if (group == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "group not found");
                return;
            }

            group.addNotification(userName, message);

            ofy().save().entities(group).now();

            // respond
            String groupJson = gson.toJson(group);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().write(groupJson);
            resp.getWriter().flush();
            resp.getWriter().close();

        }
        else if (action != null && action.equals("make_admin")) {
            String groupIdString = (String) params.get("group_id");
            if (groupIdString == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing fields");
                return;
            }
            long groupId = Long.parseLong(groupIdString);

            group = ofy().load().type(GroupData.class).id(groupId).now();
            if (group == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "group not found");
                return;
            }

            String newAdmin = (String) params.get("new_admin");
            if (newAdmin == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing new_admin field");
                return;
            }

            if (!group.getUserById(userId).isAdmin()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "must be an admin");
                return;
            }

            group.getUserById(newAdmin).setAdmin(true);

            // respond
            String groupJson = gson.toJson(group);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().write(groupJson);
            resp.getWriter().flush();
            resp.getWriter().close();

        }
        else {
            // create a new group
            long id = DatastoreServiceFactory.getDatastoreService().allocateIds("group", 1).getStart().getId();
            group = new GroupData(id, groupName);
            group.addAdmin(user);
            user.addGroup(group);
        }

        // put the data back
        ofy().save().entities(user, group).now();

        // respond
        String userJson = gson.toJson(user);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(userJson);
        resp.getWriter().flush();
        resp.getWriter().close();
    }


    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idString = req.getParameter("group_id");
        if (idString == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing group+id field");
            return;
        }

        long id = Long.parseLong(idString);

        GroupData group = ofy().load().type(GroupData.class).id(id).now();
        if (group == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "group not found");
            return;
        }

        // respond
        String groupJson = gson.toJson(group);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(groupJson);
        resp.getWriter().flush();
        resp.getWriter().close();
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> params = ServletUtils.getParams(req.getInputStream());

        String groupIdString = req.getParameter("group_id");
        String userId = req.getParameter("user_id");

        if (userId == null || groupIdString == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing parameters");
            return;
        }

        long groupId = Long.parseLong(groupIdString);
        GroupData group = ofy().load().type(GroupData.class).id(groupId).now();

        if (!group.isUserAdmin(userId)) {

            UserData user = ofy().load().type(UserData.class).id(userId).now();
            for (int i = 0; i < group.getUsers().size(); i++) {
                if (group.getUsers().get(i).getId().equals(userId)) {
                    group.getUsers().remove(i);
                    break;
                }
            }

            for (int i = 0; i < user.getGroups().size(); i++) {
                if (user.getGroups().get(i).getId() == group.getId()) {
                    user.getGroups().remove(i);
                    break;
                }
            }

            ofy().save().entities(group, user).now();

            String userJson = gson.toJson(user);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().write(userJson);
            resp.getWriter().flush();
            resp.getWriter().close();
            return;
        }

        // delete user pointers
        List<UserData> store = new ArrayList<UserData>();
        UserData caller = null;
        for (GroupUserDetailed user: group.getUsers()) {
            UserData changeMe = ofy().load().type(UserData.class).id(user.getId()).now();
            USER: for (int i = 0; i < changeMe.getGroups().size(); i++) {
                if (changeMe.getGroups().get(i).getId() == group.getId()) {
                    changeMe.getGroups().remove(i);
                    break USER;
                }
            }

            if (changeMe.getInvites() != null) {
                USER:
                for (int i = 0; i < changeMe.getInvites().size(); i++) {
                    if (changeMe.getInvites().get(i).getGroupId() == group.getId()) {
                        changeMe.getInvites().remove(i);
                        break USER;
                    }
                }
            }

            store.add(changeMe);
            if (changeMe.getId().equals(userId))
                caller = changeMe;
        }

        // delete the events
        for (GroupData.GroupEventData event: group.getEvents()) {
            ofy().delete().type(EventData.class).id(event.getEventId());
        }

        ofy().delete().type(GroupData.class).id(group.getId());

        ofy().save().entities(store);

        String userJson = gson.toJson(caller);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(userJson);
        resp.getWriter().flush();
        resp.getWriter().close();
    }

}

