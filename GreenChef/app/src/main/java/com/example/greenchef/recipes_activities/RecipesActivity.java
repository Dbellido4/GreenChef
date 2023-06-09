package com.example.greenchef.recipes_activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.greenchef.R;
import com.example.greenchef.admin_activities.ListProductActivity;
import com.example.greenchef.model.Recetas;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class RecipesActivity extends AppCompatActivity {
    private String AppId = "pruebaproyecto-urnlx";
    private MongoDatabase mongoDatabase;
    private MongoClient mongoClient;
    private List<Recetas> listaRecetas = new ArrayList<>();
    private ArrayAdapter<Recetas> adaptador;
    private Bundle bundle, bundleTipo;
    private String tipo;
    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipes);

        try {
            this.getSupportActionBar().hide();
        } catch (Exception e) {
        }

        // Mostrar un mensaje de carga
        final SweetAlertDialog dialogo = new SweetAlertDialog(RecipesActivity.this, SweetAlertDialog.PROGRESS_TYPE)
                .setTitleText("Cargando")
                .setContentText("Espere por favor...");
        dialogo.show();
        dialogo.setCancelable(false);
        new CountDownTimer(800 * 2, 800) {
            public void onTick(long millisUntilFinished) {
                i++;
                switch (i) {
                    case 0:
                        dialogo.getProgressHelper().setBarColor(getResources().getColor(cn.pedant.SweetAlert.R.color.blue_btn_bg_color));
                        break;
                    case 1:
                        dialogo.getProgressHelper().setBarColor(getResources().getColor(cn.pedant.SweetAlert.R.color.material_deep_teal_50));
                        break;
                    case 2:
                        dialogo.getProgressHelper().setBarColor(getResources().getColor(cn.pedant.SweetAlert.R.color.success_stroke_color));
                        break;
                    case 3:
                        dialogo.getProgressHelper().setBarColor(getResources().getColor(cn.pedant.SweetAlert.R.color.material_deep_teal_20));
                        break;
                    case 4:
                        dialogo.getProgressHelper().setBarColor(getResources().getColor(cn.pedant.SweetAlert.R.color.material_blue_grey_80));
                        break;
                    case 5:
                        dialogo.getProgressHelper().setBarColor(getResources().getColor(cn.pedant.SweetAlert.R.color.warning_stroke_color));
                        break;
                    case 6:
                        dialogo.getProgressHelper().setBarColor(getResources().getColor(cn.pedant.SweetAlert.R.color.success_stroke_color));
                        break;
                }
            }

            @Override
            public void onFinish() {
                i = -1;
                dialogo.dismiss();
            }
        }.start();

        bundleTipo = getIntent().getExtras();
        tipo = bundleTipo.getString("tipo");

            ListView listView = findViewById(R.id.lvProtein);
            obtenerListaRecetas(new RecetasCallback() {
                @Override
                public void onRecetasObtenidos(List<Recetas> listaRecetas) {
                    adaptador = new ArrayAdapter<Recetas>(RecipesActivity.this, R.layout.element_list_recipes, listaRecetas) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.element_list_recipes, parent, false);
                            }
                            ImageView imageView = convertView.findViewById(R.id.imgRecetas);
                            TextView nombreTextView = convertView.findViewById(R.id.txtNombre);
                            TextView tiempoTextView = convertView.findViewById(R.id.txtTiempo);

                            // Obtener el objeto Producto correspondiente a la posición en la lista
                            Recetas receta = getItem(position);

                            // Convertir los bytes en un objeto Bitmap
                            Bitmap bitmap = BitmapFactory.decodeByteArray(receta.getImagen(), 0, receta.getImagen().length);

                            // Establecer los valores de los elementos de la vista
                            imageView.setImageBitmap(bitmap);
                            nombreTextView.setText(receta.getNombre());
                            tiempoTextView.setText(receta.getTiempo());

                            return convertView;
                        }
                    };

                    listView.setAdapter(adaptador);
                    listView.setDivider(new ColorDrawable(getResources().getColor(R.color.verde04)));
                    listView.setDividerHeight(20);
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Obtener el producto seleccionado
                    Recetas recetaSeleccionada = listaRecetas.get(position);

                    // Crear el Intent
                    Intent intent = new Intent(RecipesActivity.this, RecipesCompoundActivity.class);
                    bundle = new Bundle();

                    // Pasar los datos de la receta seleccionada como extras del Intent
                    bundle.putString("nombreReceta", recetaSeleccionada.getNombre());
                    bundle.putString("descripcion", recetaSeleccionada.getDescripcion());
                    bundle.putStringArrayList("ingredientes", recetaSeleccionada.getIngredientes());
                    bundle.putString("procedimiento", recetaSeleccionada.getProcedimiento());
                    bundle.putString("tiempo", recetaSeleccionada.getTiempo());
                    bundle.putInt("porciones", recetaSeleccionada.getPorciones());
                    bundle.putByteArray("imagen", recetaSeleccionada.getImagen());

                    // Iniciar la actividad RecipesCompoundActivity
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });
    }


        private void obtenerListaRecetas (RecetasCallback callback){
            Realm.init(this);
            App app = new App(new AppConfiguration.Builder(AppId).build());

            Credentials credentials = Credentials.anonymous();
            app.loginAsync(credentials, new App.Callback<User>() {
                @Override
                public void onResult(App.Result<User> result) {
                    if (result.isSuccess()) {

                        User user = app.currentUser();
                        mongoClient = user.getMongoClient("mongodb-atlas");
                        mongoDatabase = mongoClient.getDatabase("GreenChef");
                        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("Recipes");

                        Document queryFilter = new Document().append("tipo", tipo);
                        RealmResultTask<MongoCursor<Document>> queryTask = mongoCollection.find(queryFilter).iterator();

                        queryTask.getAsync(task -> {
                            if (task.isSuccess()) {
                                MongoCursor<Document> results = task.get();
                                while (results.hasNext()) {
                                    Document recipes = results.next();
                                    String name = recipes.getString("nombre");
                                    String descripcion = recipes.getString("descripcion");
                                    ArrayList<String> ingredientes = (ArrayList<String>) recipes.get("ingredientes");
                                    String procedimiento = recipes.getString("procedimiento");
                                    String tiempo = recipes.getString("tiempo_preparacion");
                                    int porciones = recipes.getInteger("porciones");

                                    // Obtener la imagen codificada en base64 desde el campo 'image'
                                    String encodedImage = recipes.getString("img");

                                    // Decodificar la imagen de base64 a bytes
                                    byte[] imageBytes = new byte[0];
                                    if (encodedImage != null) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            imageBytes = Base64.getDecoder().decode(encodedImage);
                                        }
                                    }else
                                        Toast.makeText(RecipesActivity.this, "Imagen vacia", Toast.LENGTH_SHORT).show();

                                    // Crear una instancia de Receta con los datos obtenidos y añadirlo a la lista
                                    Recetas recetas = new Recetas(name, descripcion, ingredientes, procedimiento, tiempo, porciones);
                                    recetas.setImagen(imageBytes);
                                    listaRecetas.add(recetas);
                                }

                                callback.onRecetasObtenidos(listaRecetas);
                            } else {
                                Toast.makeText(RecipesActivity.this, "Error al buscar recetas", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(RecipesActivity.this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Interfaz de devolución de llamada para obtener la lista de recetas
        interface RecetasCallback {
            void onRecetasObtenidos(List<Recetas> listaProductos);
        }
}