package casuals.filthy.playmaker.backend;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.repackaged.com.google.api.client.util.store.DataStoreFactory;
import com.google.appengine.repackaged.com.google.api.client.util.store.DataStoreUtils;
import com.google.gson.Gson;
import com.googlecode.objectify.ObjectifyService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import data.GroupData;
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

}
