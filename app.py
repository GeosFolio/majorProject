import base64
import uuid
import flask
import json
import folium
from flask import Flask, jsonify, request, send_from_directory, abort
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.types import TypeDecorator, TEXT
from flask_apscheduler import APScheduler
from datetime import datetime, timedelta
import os
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding
from twilio.rest import Client
from flask_mail import Mail, Message
from dotenv import load_dotenv

load_dotenv("variables.env")

TWILIO_AUTH_TOKEN = os.getenv('TWILIO_AUTH_TOKEN')
TWILIO_NUMBER = os.getenv('TWILIO_PHONE_NUMBER')
TWILIO_ACCOUNT_ID = os.getenv('TWILIO_ACCOUNT_SID')

print(f"TWILIO_AUTH_TOKEN: {TWILIO_AUTH_TOKEN}")

client = Client(TWILIO_ACCOUNT_ID, TWILIO_AUTH_TOKEN)

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = (
    "postgresql://postgres:pass@localhost:5432/SaferHike"  # test_db or SaferHike
    "?sslmode=require"
    "&sslrootcert=rootCA.pem"
    "&sslcert=client.crt"
    "&sslkey=client.key"
)
app.config['MAIL_SERVER']='live.smtp.mailtrap.io'
app.config['MAIL_PORT'] = 587
app.config['MAIL_USERNAME'] = 'api'
app.config['MAIL_PASSWORD'] = '583c8b442b598ec8b3e3d8c53d5d0eaa'
app.config['MAIL_DEFAULT_SENDER'] = 'api@demomailtrap.com'
app.config['MAIL_USE_TLS'] = True
app.config['MAIL_USE_SSL'] = False
mail = Mail(app)
db = SQLAlchemy(app)
scheduler = APScheduler()
scheduler.api_enabled = True
scheduler.init_app(app)
scheduler.start()

MAPS_DIRECTORY = os.path.join(app.root_path, 'static', 'maps')


class JSONType(TypeDecorator):
    impl = TEXT

    def process_bind_param(self, value, dialect):
        if value is not None:
            return json.dumps(value)
        return value

    def process_result_value(self, value, dialect):
        if value is not None:
            return json.loads(value)
        return value


class User(db.Model):
    __tablename__ = 'users'

    uid = db.Column(db.String(255), primary_key=True)
    public_key = db.Column(db.String(500), nullable=True)
    f_name = db.Column(db.String(50), nullable=False)
    l_name = db.Column(db.String(50), nullable=False)
    emergency_contacts = db.Column(db.JSON, nullable=False, default=list)
    registration_date = db.Column(db.DateTime, default=db.func.current_timestamp())


class Hike(db.Model):
    __tablename__ = 'hike_plans'  # Table name in PostgreSQL

    pid = db.Column(db.Integer, primary_key=True, autoincrement=True)
    uid = db.Column(db.String(255), nullable=False)
    hname = db.Column(db.String(50), nullable=True)  # Updated to match the table schema
    end_time = db.Column(db.String(50), nullable=True)  # Updated to match the table schema
    supplies = db.Column(db.Text, nullable=True)
    markers = db.Column(db.JSON, nullable=True, default=list)
    traveled_path = db.Column(db.JSON, nullable=True, default=list)
    creation_date = db.Column(db.DateTime, default=db.func.current_timestamp())
    lat = db.Column(db.Float, nullable=False)
    lng = db.Column(db.Float, nullable=False)
    completed = db.Column(db.Boolean, nullable=False, default=False)
    in_progress = db.Column(db.Boolean, nullable=False, default=False)


def get_user_public_key(uid):
    print("Attempting to retrieve user public key")
    user = db.session.query(User).filter_by(uid=uid).first()
    print("User Received")
    if user and user.public_key:
        print("Attempting to create public key")
        base64_key = user.public_key.strip()
        pem_key = (
            f"-----BEGIN PUBLIC KEY-----\n{base64_key}\n-----END PUBLIC KEY-----"
        ).encode("utf-8")
        public_key = serialization.load_pem_public_key(
            pem_key, default_backend()
        )
        print("Public key made")
        return public_key
    else:
        return None


def encrypt_data(data, public_key):
    print("Attempting to encrypt data: {}".format(data))
    if not isinstance(data, bytes):
        data = data.encode('utf-8')
    encrypted_data = public_key.encrypt(
        data,
        padding.PKCS1v15()
    )
    print("Encrypted: {}".format(encrypted_data))
    return base64.b64encode(encrypted_data).decode('utf-8')


def load_private_key():
    with open("flask_private_key.pem", "rb") as key_file:
        private_key = serialization.load_pem_private_key(
            key_file.read(),
            password=None,
            backend=default_backend()
        )
        return private_key


def decrypt_data(encrypted_data, private_key):
    encrypted_data = base64.b64decode(encrypted_data)
    print("Inside decrypt_data")
    decrypted_data = None
    print("Encrypted data: {}".format(encrypted_data))
    try:
        decrypted_data = private_key.decrypt(
            encrypted_data,
            padding.PKCS1v15()
        )
        print("Decrypted data: {}".format(decrypted_data))
    except Exception as e:
        print("Decryption Failed:", str(e))
    print("Decrypted Data now returning")
    return decrypted_data.decode("utf-8")


# Function to generate the map and return its public URL
def generate_map_url(hike):
    traveled_path = hike.traveled_path
    markers = hike.markers

    if not traveled_path or not markers:
        return None

    traveled_path = [(float(location['latitude']), float(location['longitude'])) for location in traveled_path]
    intended_path = [(float(marker['lat']), float(marker['lng'])) for marker in markers]

    start_marker = traveled_path[0] if traveled_path else intended_path[0]
    map_center = start_marker
    m = folium.Map(location=map_center, zoom_start=10)

    folium.PolyLine(intended_path, color="green", weight=3, opacity=0.7, dash_array="5").add_to(m)
    for marker in markers:
        folium.Marker(
            (marker['lat'], marker['lng']),
            popup=f"{marker['title']}: {marker['description']}",
            icon=folium.Icon(color="green")
        ).add_to(m)

    folium.PolyLine(traveled_path, color="blue", weight=5).add_to(m)
    if traveled_path:
        folium.Marker(start_marker, popup="Beginning Location", icon=folium.Icon(color="green")).add_to(m)
        end_marker = traveled_path[-1]
        folium.Marker(end_marker, popup="Last Known Location", icon=folium.Icon(color="red")).add_to(m)

    # Save the map to a publicly accessible path
    if not os.path.exists(MAPS_DIRECTORY):
        os.makedirs(MAPS_DIRECTORY)  # Create directory if it doesn't exist

    map_file_name = f"map_{str(uuid.uuid4())}.html"  # Create a unique map name
    map_html_path = os.path.join(MAPS_DIRECTORY, map_file_name)

    # Save the map to an HTML file
    m.save(map_html_path)

    # Generate a public URL for the map using the dedicated route
    map_url = f"http://192.168.1.67:5000/maps/{map_file_name}"

    return map_url


# Updated check_hike_status to include additional hike details in the map
def check_hike_status(hike_id):
    with app.app_context():
        hike = Hike.query.get(hike_id)
        if not hike or hike.completed:
            return

        user = User.query.get(hike.uid)
        if not user:
            return

        # Generate notification message
        message = f"User {user.f_name} {user.l_name} did not complete the hike '{hike.hname}'.\n"
        message += f"Expected Duration: {hike.end_time}\n"
        message += f"Last Known Location: {hike.traveled_path[-1] if hike.traveled_path else 'Unknown'}\n"

        # Generate public map URL with hike details
        map_url = generate_map_url(hike)

        if map_url:
            message += f"\nView the hiker's path and details here: {map_url}\n"

        # Send notifications to emergency contacts
        for contact in user.emergency_contacts:
            email = contact.get('email')
            phone_number = contact.get('phoneNumber')
            if email:
                send_email(email, "Hiker Status Alert", message)
            if phone_number:
                send_sms(phone_number, message)
            print("Email or Phone Number not provided")


# Updated send_sms and send_email to handle only message with a map link
def send_sms(to_number, message):
    print(f"Sending SMS to {to_number}:\nMessage: {message}")
    try:
        sms = client.messages.create(
            body=message,
            from_=TWILIO_NUMBER,
            to=to_number
        )
        print(f"SMS sent to {to_number}")
    except Exception as e:
        print(f"Failed to send SMS: {e}")


def send_email(to_email, subject, message):
    print(f"Sending email to {to_email}:\nSubject: {subject}\nMessage: {message}")
    try:
        with app.app_context():
            email = Message(subject, recipients=[to_email], body=message)
            mail.send(email)
            print(f"Email sent to {to_email}")
    except Exception as e:
        print(f"Failed to send email: {e}")


@app.route('/maps/<path:filename>', methods=["GET"])
def serve_map(filename):
    try:
        return send_from_directory(MAPS_DIRECTORY, filename)
    except FileNotFoundError:
        abort(404)


@app.route('/hikes/start', methods=['PUT'])
def start_hike():
    print("Start Hike Received")
    data = request.json
    key = load_private_key()
    hike_id = data.get('pid')
    expected_return_time = decrypt_data(data.get('duration'), key)
    print("Checking hike id or return time")
    print("Hike id: {}, expected return time: {}".format(hike_id, expected_return_time))
    if not hike_id or not expected_return_time:
        return jsonify({'error': 'Hike ID and expected return time are required'}), 400
    print("Attempting to get hike")
    hike = Hike.query.get(hike_id)
    if not hike:
        return jsonify({'error': 'Hike not found'}), 404
    print("Updating hike status")
    # Update hike status
    hike.in_progress = True
    hike.completed = False
    hike.end_time = expected_return_time
    decrypted_traveled_path = [
        {
            'latitude': float(decrypt_data(latlng['latitude'], key)),
            'longitude': float(decrypt_data(latlng['longitude'], key))
        } for latlng in data['traveledPath']
    ]
    hike.traveled_path = decrypted_traveled_path
    db.session.commit()
    print("Trying to do scheduling")
    # Schedule a function to check hike status after expected return time
    try:
        hours, minutes = map(int, expected_return_time.split(':'))
        duration = timedelta(hours=hours, minutes=minutes)
        scheduler.add_job(
            func=check_hike_status,
            trigger='date',
            run_date=datetime.now() + duration,
            args=[hike_id],
            id=f'check_hike_{hike_id}'
        )
    except ValueError:
        return jsonify({'error': 'Invalid time format, expected H:M'}), 400

    return jsonify({'message': 'Hike started successfully'}), 200


@app.route('/hikes/<int:hike_id>/paths', methods=['GET'])
def get_hike_path(hike_id):
    print("Attempting to get hike path")
    try:
        hike = Hike.query.filter_by(pid=hike_id).first()

        if not hike:
            return jsonify({"error": "Hike not found"}), 404
        user_public_key = get_user_public_key(hike.uid)
        traveled_path = [
            {
                'latitude': encrypt_data(str(location['latitude']), user_public_key),
                'longitude': encrypt_data(str(location['longitude']), user_public_key)
            } for location in hike.traveled_path
        ] if hike.traveled_path else []
        print("Get hike path successful")
        return jsonify(traveled_path), 200
    except Exception as e:
        print(str(e))
        return jsonify({"error": str(e)}), 404


@app.route('/hikes', methods=['POST', 'GET', 'PUT'])
def hike_request():
    if request.method == 'GET':
        try:
            uid = flask.request.values.get('uid')
            print("Uid Received: {}".format(uid))
            hikes = db.session.query(Hike).filter_by(uid=uid).all()
            if not hikes:
                return jsonify({"error": "No hikes found for this user"}), 404
            print("Getting user public key")
            user_public_key = get_user_public_key(uid)
            if not user_public_key:
                return jsonify({"error": "User public key not found"}), 404
            print("Starting to generate hike list")
            # Encrypt the hike data using the user's public key
            hikes_list = [
                {
                    "pid": hike.pid,
                    "uid": encrypt_data(hike.uid, user_public_key),
                    "name": encrypt_data(hike.hname, user_public_key) if hike.hname else "Name Missing",
                    "supplies": encrypt_data(hike.supplies, user_public_key) if hike.supplies else "Supplies Missing",
                    "duration": encrypt_data(hike.end_time, user_public_key) if hike.end_time else "",
                    "markers": [
                        {
                            'lat': encrypt_data(str(marker['lat']), user_public_key),
                            'lng': encrypt_data(str(marker['lng']), user_public_key),
                            'title': encrypt_data(marker['title'], user_public_key),
                            'description': encrypt_data(marker['description'], user_public_key)
                        } for marker in hike.markers
                    ] if hike.markers else [],
                    "lat": encrypt_data(str(hike.lat), user_public_key),
                    "lng": encrypt_data(str(hike.lng), user_public_key),
                    "traveledPath": [],
                    "completed": hike.completed,
                    "inProgress": hike.in_progress
                }
                for hike in hikes
            ]
            return jsonify(hikes_list), 200

        except Exception as e:
            print("Error caught: {}".format(str(e)))
            return jsonify({"error": str(e)}), 500
    elif request.method == "POST":
        try:
            data = request.json
            key = load_private_key()
            print("Got Data: {}".format(data))
            if not data:
                return jsonify({"error": "No data provided"}), 400

            # Validate required fields
            required_fields = ['uid', 'supplies', 'markers', 'name', 'pid']
            for field in required_fields:
                if field not in data:
                    return jsonify({"error": f"Missing field: {field}"}), 400

            print("Validated Fields")
            print("Attempting to decrypt markers")
            decrypted_markers = []
            for marker in data['markers']:
                decrypted_marker = {
                    'lat': float(decrypt_data(marker['lat'], key)),
                    'lng': float(decrypt_data(marker['lng'], key)),
                    'title': decrypt_data(marker['title'], key),
                    'description': decrypt_data(marker['description'], key)
                }
                decrypted_markers.append(decrypted_marker)
            print("Decrypted markers: {}".format(decrypted_markers))
            # Create a new hike
            hike = Hike(
                uid=decrypt_data(data['uid'], key),
                hname=decrypt_data(data['name'], key),
                supplies=decrypt_data(data['supplies'], key),
                markers=decrypted_markers,
                lat=float(decrypt_data(data['lat'], key)),
                lng=float(decrypt_data(data['lng'], key))
            )
            db.session.add(hike)
            print("Creating new hike")

            try:
                db.session.commit()
                return jsonify({"message": "Hike processed successfully!"}), 201
            except Exception as e:
                db.session.rollback()
                print(e)
                return jsonify({"error": "Failed to commit the hike to the database"}), 500

        except Exception as e:
            print("Error caught: {}".format(str(e)))
            db.session.rollback()
            return jsonify({"error": str(e)}), 400
    else:
        try:
            print("Updating hike")
            data = request.json
            key = load_private_key()
            hike = db.session.query(Hike).filter_by(pid=data['pid']).first()
            if hike:
                print("Decrypting markers")
                decrypted_markers = [
                    {
                        'lat': float(decrypt_data(marker['lat'], key)),
                        'lng': float(decrypt_data(marker['lng'], key)),
                        'title': decrypt_data(marker['title'], key),
                        'description': decrypt_data(marker['description'], key)
                    } for marker in data['markers']
                ]
                print("Decrypting traveled path")
                decrypted_traveled_path = [
                    {
                        'latitude': decrypt_data(latlng['latitude'], key),
                        'longitude': decrypt_data(latlng['longitude'], key)
                    } for latlng in data['traveledPath']
                ]
                print("Final Decrypts")
                # Update the existing hike fields
                hike.hname = decrypt_data(data['name'], key)
                hike.end_time = decrypt_data(data['duration'], key)
                hike.supplies = decrypt_data(data['supplies'], key)
                hike.markers = decrypted_markers
                hike.lat = float(decrypt_data(data['lat'], key))
                hike.lng = float(decrypt_data(data['lng'], key))
                hike.traveled_path = decrypted_traveled_path
                hike.completed = data['completed']
                hike.in_progress = data['inProgress']
            else:
                return jsonify({"error": "No hike found with this pid"}), 404
            db.session.commit()
            return jsonify({"message": "Hike updated successfully!"}), 200
        except Exception as e:
            print("Error caught: {}".format(str(e)))
            db.session.rollback()
            return jsonify({"error": str(e)}), 400


@app.route("/hikes/<int:pid>", methods=["DELETE"])
def delete_hike(pid):
    try:
        print("Delete hit")
        pid = pid
        # Find the hike by pid
        hike = Hike.query.get(pid)

        if not hike:
            return jsonify({"error": f"Hike with pid {pid} not found"}), 404

        # Delete the hike from the database
        db.session.delete(hike)
        db.session.commit()

        return jsonify({"message": f"Hike with pid {pid} deleted successfully"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/users', methods=['POST', 'PUT', 'GET'])
def handle_user():
    if request.method == "POST":
        try:
            print("Creating new user")
            data = request.json
            key = load_private_key()
            print(data)
            decrypted_emergency_contacts = [
                {
                    'phoneNumber': decrypt_data(contact["phoneNumber"], key),
                    'email': decrypt_data(contact["email"], key)
                } for contact in data["emergencyContacts"]
            ]
            # Create new User instance
            uid = decrypt_data(data['uid'], key)
            new_user = User(
                uid=uid,
                public_key=data["publicKey"],
                f_name=decrypt_data(data['fName'], key),
                l_name=decrypt_data(data['lName'], key),
                emergency_contacts=decrypted_emergency_contacts
            )
            print("Attempting to add user")
            db.session.add(new_user)
            db.session.commit()
            return jsonify({"message": "User created successfully!"}), 201

        except Exception as e:
            db.session.rollback()
            print(str(e))
            return jsonify({"error": str(e)}), 500
    elif request.method == "PUT":
        try:
            print("Put hit")
            data = request.json
            key = load_private_key()
            uid = decrypt_data(data.get('uid'), key)

            user = User.query.filter_by(uid=uid).first()

            if not user:
                return jsonify({"error": "User not found"}), 404
            decrypted_emergency_contacts = [
                {
                    'phoneNumber': decrypt_data(contact["phoneNumber"], key),
                    'email': decrypt_data(contact["email"], key)
                } for contact in data["emergencyContacts"]
            ]
            # Update user details
            user.f_name = decrypt_data(data['fName'], key)
            user.l_name = decrypt_data(data['lName'], key)
            user.emergency_contacts = decrypted_emergency_contacts
            user.public_key = data['publicKey']

            db.session.commit()
            return jsonify({"message": "User updated successfully!"}), 200

        except Exception as e:
            print("Error: " + str(e))
            db.session.rollback()
            return jsonify({"error": str(e)}), 40
    else:
        print("Get user hit")
        uid = request.args.get('uid')
        if not uid:
            return jsonify({'error': 'UID is required'}), 400

        try:
            user = User.query.filter_by(uid=uid).first()
            if user:
                # Assuming the User model has attributes uid, fName, lName, and emergencyContacts
                user_public_key = get_user_public_key(uid)
                if not user_public_key:
                    return jsonify({"Error": "User public key not found"}), 404

                    # Encrypt user data fields using the public key
                encrypted_user_data = {
                    "uid": encrypt_data(user.uid, user_public_key),
                    "fName": encrypt_data(user.f_name, user_public_key),
                    "lName": encrypt_data(user.l_name, user_public_key),
                    "publicKey": user.public_key,
                    "emergencyContacts": [
                        {
                            "email": encrypt_data(contact['email'],
                                                  user_public_key) if 'email' in contact else None,
                            "phoneNumber": encrypt_data(contact['phoneNumber'],
                                                        user_public_key) if 'phoneNumber' in contact else None
                        } for contact in user.emergency_contacts
                    ]
                }
                return jsonify(encrypted_user_data), 200
            else:
                return jsonify({'error': f'User with uid {uid} not found'}), 404

        except Exception as e:
            return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
