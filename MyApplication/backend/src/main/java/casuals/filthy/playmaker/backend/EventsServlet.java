package casuals.filthy.playmaker.backend;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import data.EventData;
import data.GroupData;
import data.UserData;

import static casuals.filthy.playmaker.backend.OfyService.ofy;

/**
 * Created by Chris on 3/27/2015.
 */
public class EventsServlet extends HttpServlet {


    public static Gson gson = new Gson();

    /**
     * Create a new event
     * @param resp
     * @throws IOException
     */
    private void createEvent(HashMap<String, String> params, HttpServletResponse resp) throws IOException {
        String userId = params.get("user_id");
        String groupIdString = params.get("group_id");
        String name = params.get("event_name");
        String type = params.get("event_type").toLowerCase();
        String dateString = params.get("event_date");
        String address = params.get("event_address");

        if (userId == null || groupIdString == null || name == null || type == null || dateString == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing required field: user_id, group_id, event_name, event_type, event_date");
            return;
        }
        long groupId = Long.parseLong(groupIdString);
        long date = Long.parseLong(dateString);

        // get the group
        GroupData group = ofy().load().type(GroupData.class).id(groupId).now();
        if (group == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "group not found");
            return;
        }

        // check for admin rights
        if (!group.isUserAdmin(userId)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "user is not group admin");
            return;
        }

        // create the event
        long id = DatastoreServiceFactory.getDatastoreService().allocateIds("event", 1).getStart().getId();
        EventData event = new EventData(id, date, type, group.getId(), name, address);

        group.addEvent(event);

        // put the data back
        ofy().save().entities(event, group).now();

        // respond
        String groupJson = gson.toJson(event);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(groupJson);
        resp.getWriter().flush();
        resp.getWriter().close();
    }

    /**
     * Get an existing event
     * @param req
     * @param resp
     * @throws IOException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String groupIdString = req.getParameter("group_id");
        String eventIdString = req.getParameter("event_id");

        if (groupIdString == null || eventIdString == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing required field: group_id, event_id");
            return;
        }

        long groupId = Long.parseLong(groupIdString);
        long eventId = Long.parseLong(eventIdString);

        EventData event = ofy().load().type(EventData.class).id(eventId).now();
        if (event == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "event not found");
            return;
        }

        if (event.getGroupId() != groupId) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "incorrect group id");
            return;
        }

        // respond
        String groupJson = gson.toJson(event);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(groupJson);
        resp.getWriter().flush();
        resp.getWriter().close();
    }

    /**
     * Make changes to an existing event
     * @param req
     * @param resp
     * @throws IOException
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, String> params = ServletUtils.getParams(req.getInputStream());
        String groupIdString = params.get("group_id");//req.getParameter("group_id");
        String eventIdString = params.get("event_id");//req.getParameter("event_id");

        // optional stuff
        String userId = params.get("user_id");//req.getParameter("user_id");
        String name = params.get("event_name");//req.getParameter("event_name");
        String type = params.get("event_type").toLowerCase();//req.getParameter("event_type");
        String dateString = params.get("event_date");//req.getParameter("event_date");
        String address = params.get("event_address");

        if (groupIdString == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing required field: group_id");
            return;
        }

        if (eventIdString == null) {
            // create a new event
            createEvent(params, resp);
            return;
        }

        long groupId = Long.parseLong(groupIdString);
        long eventId = Long.parseLong(eventIdString);

        EventData event = ofy().load().type(EventData.class).id(eventId).now();
        if (event == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "event not found");
            return;
        }
        GroupData group = ofy().load().type(GroupData.class).id(groupId).now();

        if (event.getGroupId() != groupId) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "incorrect group id");
            return;
        }

        // add user
        if (userId != null) {
            UserData user = ofy().load().type(UserData.class).id(userId).now();
            if (user == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "user not found");
                return;
            }
            event.addAttendee(user.getId(), user.getName());
        }

        // admin abilities
        if (name != null || type != null || dateString != null) {
            // check to make sure admin
            if (!group.isUserAdmin(userId)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "user is not admin");
                return;
            }

            if (name != null) {
                group.getEvent(eventId).setName(name);
                event.setName(name);
            }

            if (type != null) {
                event.setType(type);
            }

            if (address != null) {
                event.setAddress(address);
            }

            if (dateString != null) {
                long date = Long.parseLong(dateString);
                event.setDate(date);
                group.getEvent(eventId).date = date;
            }

            ofy().save().entity(group).now();
        }

        // put the data back
        ofy().save().entity(event).now();

        // respond
        String groupJson = gson.toJson(event);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write(groupJson);
        resp.getWriter().flush();
        resp.getWriter().close();
    }
}
