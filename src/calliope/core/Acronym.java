/* This file is part of calliope.
 *
 *  calliope is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) 2016 Desmond Schmidt
 */
package calliope.core;
import java.util.HashMap;
import calliope.core.database.Connector;
import calliope.core.database.Connection;
import calliope.core.exception.DbException;
import calliope.core.constants.Database;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.HashSet;

/**
 *
 * @author desmond
 */
public class Acronym {
    static HashMap<String,String> acronyms;
    static HashSet<String> loaded;
    /**
     * Expand an acronym
     * @param projid the acronym's project
     * @param acronym
     * @return 
     */
    public static String expand( String projid, String acronym )
    {
        String key = projid + "/" + acronym;
        if ( acronyms == null )
            acronyms = new HashMap<String,String>();
        if ( loaded == null )
            loaded = new HashSet<String>();
        if ( acronyms.containsKey(key) )
            return acronyms.get(key);
        else
        {
            try
            {
                if ( !loaded.contains(projid) )
                {
                    Connection conn = Connector.getConnection();
                    String jStr = conn.getFromDb(Database.ACRONYMS, projid );
                    if ( jStr != null )
                    {
                        JSONObject jObj = (JSONObject)JSONValue.parse(jStr);
                        JSONArray jArr = (JSONArray)jObj.get("acronyms");
                        for ( int i=0;i<jArr.size();i++ )
                        {
                            JSONObject obj = (JSONObject)jArr.get(i);
                            String aKey = projid + "/" + obj.get("key");
                            acronyms.put( aKey, (String)obj.get("rep"));
                        }
                        // stop it reloading if another acronym is not found
                        loaded.add(projid);
                        if ( acronyms.containsKey(key) )
                            return acronyms.get(key);
                        else
                        {
                            // stop a repeat lookup
                            acronyms.put(key,acronym);
                            return acronym;
                        }
                    }
                }
                return acronym;
            }
            catch ( DbException dbe )
            {
                return acronym;
            }
        }
    }
}
