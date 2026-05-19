const { MongoClient } = require('mongodb');

async function main() {
    const uri = "mongodb://localhost:27017/gocartacho_db";
    const client = new MongoClient(uri);

    try {
        await client.connect();
        const db = client.db("gocartacho_db");
        
        // 1. Get unique comercio_id from resenas
        const uniqueComercioIdsInResenas = await db.collection("resenas").distinct("comercio_id");
        console.log("Unique comercio_id in resenas:", uniqueComercioIdsInResenas);

        // 2. Get unique usuario_id from resenas
        const uniqueUsuarioIdsInResenas = await db.collection("resenas").distinct("usuario_id");
        console.log("Unique usuario_id in resenas:", uniqueUsuarioIdsInResenas.slice(0, 20), "... total:", uniqueUsuarioIdsInResenas.length);

        // 3. Get all comercios (id and name)
        const comercios = await db.collection("comercios").find({}, { projection: { nombre: 1 } }).toArray();
        console.log("All comercios in DB:", comercios.map(c => ({ id: c._id.toString(), nombre: c.nombre })));

        // 4. Get all users (id and username/email)
        const usuarios = await db.collection("usuarios").find({}, { projection: { username: 1, email: 1 } }).toArray();
        console.log("All users in DB:", usuarios.map(u => ({ id: u._id.toString(), username: u.username || u.email })));

    } catch (e) {
        console.error("Error:", e);
    } finally {
        await client.close();
    }
}

main();
