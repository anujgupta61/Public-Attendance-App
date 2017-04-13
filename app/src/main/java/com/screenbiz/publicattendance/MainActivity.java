package com.screenbiz.publicattendance;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> names = new ArrayList<String>() ;
    private ArrayList<Integer> attendance = new ArrayList<Integer>() ;
    private ArrayList<Integer> total_attendance = new ArrayList<Integer>() ;
    private ArrayList<Integer> points = new ArrayList<Integer>() ;
    private ArrayList<Integer> total_points = new ArrayList<Integer>() ;
    private ArrayList<Integer> ids = new ArrayList<Integer>() ;
    private CustomListAdapter adapter ;
    private SwipeRefreshLayout mySwipeRefreshLayout ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh) ;
        fetchDataRemote() ;
        mySwipeRefreshLayout.setOnRefreshListener(
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    fetchDataRemote();
                    mySwipeRefreshLayout . setRefreshing(false) ;
                    //finish();
                    //startActivity(getIntent());
                }
            }
        );
    }

    private void fetchDataRemote() {
        class wrapper {
            int id ;
            String name ;
            int att ;
            int tot_att ;
            int points ;
            int tot_points ;
        }

        class SendPostReqAsyncTask extends AsyncTask<String, Void, wrapper[]> {
            wrapper[] w;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mySwipeRefreshLayout . setRefreshing(true) ;
            }

            @Override
            protected wrapper[] doInBackground(String... params) {

                String data = "data";

                BufferedReader reader = null;
                HttpURLConnection conn = null;

                // Send data
                try {

                    // Defined URL  where to send data
                    URL url = new URL("https://screenbiz.000webhostapp.com/fetch_attendance.php");

                    // Send POST data request

                    conn = (HttpURLConnection) url.openConnection();

                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(data);
                    wr.flush();


                    // Get the server response

                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    int fetchedData = reader.read();
                    // Reading json string from server
                    String json_str = "{ \"attendance\": ";
                    while (fetchedData != -1) {
                        char current = (char) fetchedData;
                        fetchedData = reader.read();
                        json_str = json_str + current;
                    }

                    json_str = json_str + "}";
                    Log . v ("Test2" , json_str) ;
                    final JSONObject obj = new JSONObject(json_str);
                    final JSONArray geodata = obj.getJSONArray("attendance");
                    final int n = geodata.length();
                    if (n == 0)
                        return null;
                    w = new wrapper[n];
                    //int lastId = SaveSharedPreferences . getLastId(getApplicationContext()) ;
                    for (int i = 0; i < n; i++) {
                        final JSONObject att_status = geodata.getJSONObject(i);
                        w[i] = new wrapper() ;
                        w[i].id = att_status . getInt("id") ;
                        w[i].name = att_status . getString("name") ;
                        w[i].att = att_status . getInt("att") ;
                        w[i].tot_att = att_status . getInt("tot_att") ;
                        w[i].points = att_status . getInt("points") ;
                        w[i].tot_points = att_status . getInt("tot_points") ;
                    }

                } catch (Exception j) {
                    j . printStackTrace();
                    Log . v ("Test1" , "WTF") ;
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                        try {
                            reader.close();
                        } catch (Exception e) {

                        }
                    }
                }
                return w;
            }

            @Override
            protected void onPostExecute(wrapper[] w) {
                super.onPostExecute(w) ;
                mySwipeRefreshLayout . setRefreshing(false) ;
                //dialog . dismiss() ;
                if(w != null) {
                    ids . clear() ;
                    names . clear() ;
                    attendance . clear() ;
                    total_attendance . clear() ;
                    points . clear();
                    total_points .clear();
                    try {
                        for(int i = 0 ; i < w . length ; i ++) {
                            ids . add(w[i].id) ;
                            names . add(w[i].name) ;
                            attendance . add(w[i].att) ;
                            total_attendance . add(w[i].tot_att) ;
                            points . add(w[i].points) ;
                            total_points . add(w[i].tot_points) ;
                        }
                    } catch (Exception ex) {
                        Log.v("Test" , "Exception") ;
                        ex . printStackTrace();
                    }
                    adapter = new CustomListAdapter(MainActivity.this , names , attendance , total_attendance , points , total_points) ;
                    ListView listview = (ListView) findViewById(R.id.listview) ;
                    try {
                        listview.setAdapter(adapter);
                    } catch(Exception ex) {

                    }
                }
            }
        }
        if(checkConnection(getApplicationContext())) {
            SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
            sendPostReqAsyncTask.execute();
        } else {
            Toast. makeText(getApplicationContext() , "No internet" , Toast . LENGTH_SHORT) . show() ;
        }
    }

    boolean checkConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected ;
    }
}