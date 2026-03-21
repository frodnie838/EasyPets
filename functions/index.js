const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

exports.enviarNotificacionPush = functions.database.ref('/notificaciones/{usuarioId}/{idNotificacion}')
    .onCreate(async (snapshot, context) => {

        const notificacion = snapshot.val();
        const usuarioReceptorId = context.params.usuarioId;

        console.log(`Nueva notificación para: ${usuarioReceptorId}`);

        const tokenSnapshot = await admin.database().ref(`/usuarios/${usuarioReceptorId}/fcmToken`).once('value');
        const fcmToken = tokenSnapshot.val();

        if (!fcmToken) {
            console.log("El usuario no tiene FCM Token.");
            return null;
        }

        const mensajePush = {
            notification: {
                title: notificacion.titulo || "Nueva notificación",
                body: notificacion.mensaje || "Tienes un mensaje nuevo en EasyPets.",
            },
            data: {
                tipoNotif: String(notificacion.tipo || "default"),
                hiloId: String(notificacion.hiloId || ""),
                hiloTitulo: String(notificacion.hiloTitulo || ""),
                hiloDescripcion: String(notificacion.hiloDescripcion || ""),
                hiloAutor: String(notificacion.hiloAutor || ""),
                hiloTimestamp: String(notificacion.hiloTimestamp || "0")
            },
            token: fcmToken
        };

        try {
            const respuesta = await admin.messaging().send(mensajePush);
            console.log("¡Notificación enviada con ÉXITO a la pantalla bloqueada!", respuesta);
        } catch (error) {
            console.error("Error al enviar la notificación Push:", error);
        }

        return null;
    });