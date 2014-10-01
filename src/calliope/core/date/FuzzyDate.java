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
 */
package calliope.core.date;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Arrays;
import java.util.GregorianCalendar;
import org.json.simple.JSONObject;
/**
 * An imprecise date object
 * @author desmond
 */
public class FuzzyDate implements Comparable
{
    int day;
    int month;
    int year;
    Locale locale;
    Qualifier q;
    /*by 8 March 1850
    1845?
    c 1866-68*/
    /**
     *  Try to parse a year
     *  @param year the year as a string
     *  @return 0 if it failed, otherwise the year
     */
    private int parseYear( String year )
    {
        int y = 0;
        if ( year != null && year.length()>0 )
        {
            try
            {
                y = Integer.parseInt(year);
            }
            catch ( Exception e )
            {
            }
        }
        return y;
    }
    /**
     *  Try to parse a month
     *  @param q the qualifier that may modify the return value
     *  @param month a string representation of the month or rubbish
     *  @return the month's numerical value in Calendar or Integer.MAX_VALUE on failure
     */
    private int parseMonth( Qualifier q, String month )
    {
        int m = Integer.MAX_VALUE;
        if ( month != null && month.length()> 0 )
        {
            try
            {
                Date date = new SimpleDateFormat("MMM",locale).parse(month);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                m = cal.get(Calendar.MONTH);
            }
            catch ( Exception e )
            {
                switch ( q )
                {
                    case circa:
                    case early:
                        m = Calendar.JANUARY-2;
                        break;
                    case late:
                        m = Calendar.DECEMBER+1;
                        break;
                    case by:
                        m = Calendar.JANUARY-3;
                        break;
                    case perhaps:
                        m = Calendar.JANUARY-1;
                        break;
                    case none:
                        break;
                }
            }
        }
        return m;
    }
    /**
     *  Parse a day
     *  @param q the qualifier on the date
     *  @param day the numerical day as a string
     *  @return the day number -1-32 or 0 on failure 
     */
    private int parseDay( Qualifier q, String day )
    {
        int d = 0;
        if ( day != null && day.length()>0 )
        {
            try
            {
                d = Integer.parseInt(day);
                if ( d < 0 || d > 31 )
                {
                    d = 0;
                    throw new NumberFormatException();
                }
            }
            catch ( Exception e )
            {
                switch ( q )
                {
                    case circa:
                        d = -1;
                        break;
                    case early:
                        d = -1;
                        break;
                    case late:
                        d = 32;
                        break;
                    case by:
                        d = -2;
                        break;
                    case perhaps:
                        d = -1;
                        break;
                    case none:
                        break;
                }
            }
        }
        return d;
    }
    /**
     * Initialise a Fuzzy date from its saved JSON representation
     * @param obj the object
     * @param language the name of the language as a 2-character code
     */
    public FuzzyDate( JSONObject obj, String language )
    {
        if( language == null || language.length()==0 )
            this.locale = Locale.getDefault();
        else
            this.locale = new Locale(language);
        this.day = ((Number)obj.get("day")).intValue();
        this.month = ((Number)obj.get("month")).intValue();
        this.year = ((Number)obj.get("year")).intValue();
        this.q = Qualifier.parse((String)obj.get("qualifier"),locale);
    }
    
    /**
     *  Create a fuzzy date object using a restricted spec
     *  @param spec a restricted date format: 
     *  [?|By|Circa|c.|c|Early|Late] year
     *  or [?|By|Circa|c.|c|Early|Late] month year
     *  or [?|By|Circa|c.|c|Early|Late] day month year
     * @param language
     */
    public FuzzyDate( String spec, String language )
    {
        int state = 0;
        int y,d,m;
        if( language == null || language.length()==0 )
            this.locale = Locale.getDefault();
        else
            this.locale = new Locale(language);
        String[] parts = spec.split("[ -/]");
        // leading ? not separated from text
        for ( int i=0;i<parts.length;i++ )
        {
            if ( parts[i].startsWith("?") && parts[i].length()>1 )
            {
                String[] newParts = new String[parts.length+1];
                for ( int j=0;j<i;j++ )
                    newParts[j] = parts[j];
                newParts[i] = "?";
                newParts[i+1] = parts[i].substring(1);
                for ( int j=i+1;j<parts.length;j++ )
                    newParts[j+1] = parts[j];
                parts = newParts;
                break;
            }
        }
        int i = 0;
        while ( i < parts.length && state >= 0 )
        {
            switch ( state )
            {
                case 0: // look for qualifier
                    this.q = Qualifier.parse( parts[i],locale );
                    if ( this.q != Qualifier.none )
                        i++;
                    state = 1;
                    break;
                case 1: // look for day
                    d = parseDay( q, parts[i] );
                    if ( d != 0 )
                        this.day = d;
                    if ( d > 0 && d < 32 )
                        i++;
                    state = 2;
                    break;
                case 2: // look for month
                    m = parseMonth( q, parts[i] );
                    if ( m != Integer.MAX_VALUE )
                        this.month = m;
                    if ( m >= Calendar.JANUARY && m <= Calendar.DECEMBER )
                        i++;
                    else if ( m == Integer.MAX_VALUE )
                        this.month = Calendar.JANUARY-1;
                    state = 3;
                    break;
                case 3: // look for year
                    y = parseYear( parts[i] );
                    if ( y != 0 )
                    {
                        i++;
                        this.year = y;
                    }
                    state = -1;
                    break;
            }
        }
    }
    /**
     * Compare two fuzzy dates
     * @param other the other fuzzy date
     * @return -1 if this is less than other, or 1 if vice versa else 0
     */
    public int compareTo( Object b )
    {
        if ( b instanceof FuzzyDate )
        {
            FuzzyDate other = (FuzzyDate)b;
            if ( this.year < other.year )
                return -1;
            else if ( this.year > other.year )
                return 1;
            else if ( this.month < other.month )
                return -1;
            else if ( this.month > other.month )
                return 1;
            else if ( this.day < other.day )
                return -1;
            else if ( this.day > other.day )
                return 1;
            else
                return 0;
        }
        return 0;
    }
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if ( this.q == Qualifier.perhaps )
            sb.append("?");
        else if ( this.q != Qualifier.none )
            sb.append( this.q );
        if ( this.day > 0 && this.day < 32 )
        {
            if ( sb.length() > 0 )
                sb.append(" ");
            sb.append( this.day );
        }
        if ( this.month >= 0 && this.month < 13 )
        {
            Calendar c = Calendar.getInstance();
            c.set( Calendar.MONTH, this.month );
            if ( sb.length() > 0 )
                sb.append(" ");
            sb.append( c.getDisplayName(Calendar.MONTH,
                Calendar.LONG,Locale.getDefault()) );
        }
        if ( sb.length() > 0 )
            sb.append(" ");
        sb.append( this.year );
        return sb.toString();
    }
    /**
     * Get the last day of the month
     * @return the day 28-31
     */
    int lastDay()
    {
        int actualMonth = (month<0)?0:(month>11)?12:month;
        Calendar mycal = new GregorianCalendar(year, actualMonth, 1);
        return mycal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    /**
     * Convert to comma-separate format used by TimelineJS
     * @return a string
     */
    public String toCommaSep()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(year));
        sb.append(",");
        String monthStr;
        if ( month < 0 )
            monthStr = "1";
        else if ( month > Calendar.DECEMBER )
            monthStr = "12";
        else
            monthStr = Integer.toString(month+1);
        sb.append(monthStr);
        String dayStr;
        int last = lastDay();
        if ( day<=0 )
            dayStr = "1";
        else if ( day > last )
            dayStr = Integer.toString(last);
        else
            dayStr = Integer.toString(day);
        sb.append(",");
        sb.append(dayStr);
        return sb.toString();
    }
    /**
     * Convert to JSON string format
     * @return a JSON object
     */
    public String toJSON()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append("\"qualifier\": \"");
        sb.append(this.q.toString());
        sb.append("\", ");
        sb.append("\"day\": ");
        sb.append(this.day);
        sb.append(", ");
        sb.append("\"month\": ");
        sb.append(this.month);
        sb.append(", ");
        sb.append("\"year\": ");
        sb.append(this.year);
        sb.append(" }");
        return sb.toString();
    }
    public static void main( String[] args )
    {
        FuzzyDate[] fds = new FuzzyDate[14];
        fds[0] = new FuzzyDate("16-Gen-1861","it");
        fds[1] = new FuzzyDate("1833",null);
        fds[2] = new FuzzyDate("Early 1833",null);
        fds[3] = new FuzzyDate("January 1833",null);
        fds[4] = new FuzzyDate("1 January 1833",null);
        fds[5] = new FuzzyDate("2 January 1833",null); 
        fds[6] = new FuzzyDate("31 December 1833",null);
        fds[7] = new FuzzyDate("Late December 1833",null);
        fds[8] = new FuzzyDate("1834",null);
        fds[9] = new FuzzyDate("c. January 1890",null);
        fds[10] = new FuzzyDate("January 1890",null);
        fds[11] = new FuzzyDate("1 January 1890",null);
        fds[12] = new FuzzyDate("By December 1833",null);
        fds[13] = new FuzzyDate("?1833",null);
        Arrays.sort( fds );
        for ( int i=0;i<fds.length;i++ )
            System.out.println(fds[i].toCommaSep());
    }
}