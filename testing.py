import unittest
import pytest
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from app import app, db, User, Hike


class FlaskTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Use a different database for testing purposes
        app.config['SQLALCHEMY_DATABASE_URI'] = (
            "postgresql://postgres:pass@localhost:5432/test_db"
            "?sslmode=require"
            "&sslrootcert=rootCA.pem"
            "&sslcert=client.crt"
            "&sslkey=client.key"
        )
        app.config['TESTING'] = True
        cls.app = app.test_client()
        with app.app_context():
            db.create_all()

    @classmethod
    def tearDownClass(cls):
        # Clean up the database after all tests
        with app.app_context():
            db.session.remove()
            db.drop_all()

    def setUp(self):
        # Setup test data
        self.user_data = {
            "uid": "YfvA6mj1p/aKIweOk7IKfrcJ92KXiInw4l0Bmhhz6hBz1WNG0SNH+1BGMGevMQVutgvdw/ok1R2STq8ZhJtecABVAiLPEs5rylEf3hQKVr0J+yMNku2ELNIOTGYoFGgq8TzaUixNR+woR4Ud098UeUhx2/9mBhvZlEvj/fo27KiKGEIhCC0dphzej5MoneAIf2Xz/qybZWMfGzeNEBzDIyitoWpWvqidKuOvABuSz3z4GIFzNeJBCjXwIYrrLKKcMqkEbxKonh8XSmIQON4YkGC5iHjDHtmBi43h5IrQzKDPlF8P3Pnr406rHMxfLJJ7sYITEW0F4r0XsWLedEgOxQ==",
            "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApcGSi8vWxK89sgcwBv2licRE9KZR5PiEst7qpsn0hmmrDGvteoKKdHAHD/vmNFo3mLzMWGU6/2Tqryo4aL/FOwbrqWa58iKde2TFT5XIYj3pumC0JUdFqtGeNcZsOxtzcxQT25w/qwCIbo05ESBxmX+r3aJVT7/ylnXrzaePY+Fh6IAgx4+dr9wBgckBKTwR+omkl2yAPFcxYgLeWQuLOdzyb2U2YWHS5EzKX6Hk0qmmufKkB6lSwVf0PLBb7Kz27y/pX0x39l6jXeTdyEvNrhGE/j487a3HPKgIE8lFUsvjYe6pAV/wS/I5EH3/iDZM+A8L1y+nIykDKj+mQ5aQywIDAQAB",
            "fName": "JOsJFNLui3qChVimhGkDi+vrZzpZrg5diX9Ech/Mm3qBR61Q7APPx8FFX2CLCqz+7LyaozpJKadrl/w9ICwtWfwSXloU2fVpt3pzibcuZIzkPqsWFepDZSOqU58Vc2QA5SYc9kevg4f7J9/ow67ex10fbl6HuYQPPN1FNnOVLgByUlHJolmd3/Y/vznHo+FAiKBx49EM7RylAhLejojxFLnu6x+ncbN9aA6Imffs+erWt/L/EjlUza+tIp5KZtk5cADEjGd6hTH9qkdkfu8A+BuNi/zhPEwIumJbi8rBn44kpW3vyjHWVSIYr4rBFZ3b60eJisUTpGLBTGSVl1Cazw==",
            "lName": "FrIKsce0TZBn0GTC/qjrBgvUYy4lPmzSTQ1q02n8go8si+y/xyjmcKQUnJxXiMizXvNb6N5hFakij/+GS7HdjVWOApOIZBom+GKkpBU1YhDMARX2vKP/gnPy4ik046GlBhVax9THU8jJrVJKHnCR2M43Lg2EMVX3oFS62FiE9/SS3e7idX0Jtg5NHU+0werUB2PwSjTV+7RetHi71VbdJXt3d2S+/yQv6E5ZzVMB9CvV2lyxry1s5dlVcV6LemDWOmBBnqmNB/ZuLM23RfACWmfPcffEtB6uL/9gjmNoKijwxakKavEx9XI09C3ZDZM9xY7mD4vR6K+Iv3h3xWFjdw==",
            "emergencyContacts": [
                {
                    "phoneNumber": "IUIznJQJWPSDNSYYkEJPSLWl9JeqoHqRV/yrawxXuv5PXk4Ob7KDjUitw0CvmFr5ER04xCdZjdh9OxFxcAG1bAj+j9BbRE7brEiv6mff1H39iGiVxZ6RQZZ44Cw5jJUJiiYvb2dAhqTuVhjPSW21T4Wg5r6Ck1k4+GLeA99/gE0D2A11TGOkoyAw//JMGBJQmp4SxLcb79xItWuhVsehKdsnls7YbCXfQQU6Ul8X9jpIFSqMhwCKJ41bwdgB4HyHGaQK06EOb5y/MPUNbDx69rmJggd5q43R0aEFE9tG2o1GycbBnGs9ANkRJS5j6jwIHk+Q5Gz75NIW/pABbhdmbw==",
                    "email": "aQvxSasROivlVlhUDECMPr8owEbH+aNc8VjiJipY4wWF1AVFJAIo4+kGehi7n921yD1X5bacTN6E/dQRIgBHzE/4IOb6VJlmJj4N6T1fx+2eFdGmDm137X3kdZfYewka9Nokkvc4ZyRmohDkao/93kuVRNENew+gaWccGUNOiZwu+Q82vTMKaITy3zvux3HqBa9y+rEf6mRBNBjiYl/a6/p4G6B+RSFvWmB9YI55BoWLGb7ZoO0U/cde8kStOUPGjIPAS18Cq2gZeMlUah1RV22hO6rtGF5KRlp6VMQ6W/Zr+U+Oca+pOj618LcCPjJD8DPkrP7UWnfE1cpeO4Gdvw=="
                }
            ]
        }

        self.hike_data = {
            "pid": 1,
            "uid": "Go0CececmHPzutgRYYDLqqJW/ihI9zUwDOdvm+m89ArX4mWcJGV2c9fwzJZNZ0zDBW9sHdNRqBHG51HTuAYYQ7EnzCwxCU4tWAkGK+KVARsSdJTHHV0lhXhFHfvLHhYlVQqzzewmX/pvP7FPTcf6vhB3kSar4gNpKJ+Mgba0lA3dorDknXQTroFUh9Xx5FEyNZMkq/SYBVeEqtTWgvx8o4UT3y1GiyTjqsD0dYzLJEmbU9Fada/oIb6B5Ib2KR0cYS0QlMudkHHQcLhDeY9uqMgEX0GDJTZ5b7fW6JDMJ+TU9GfefZGgTsm3XZ04EFME3bWh419AR1y81R+i2JMSfg==",
            "name": "BcnovVRcj520qbeoqohbHj9CDYafL+g9Az0OEDyOvqcjClwDfsf8Zf16Gd7EMFysw5yabsncN/oIfL27plCihYX7C7MgRHAggSjrVLynLmWQOelHGYbub/Xu+6Kc5MDfUothTM46LHlv6/CLWTesFlK4/oXsMts+kvtWQCI0cAHjDq39aD94WOqeh/9Yg52m9ZBCbzFKbySaZEQNgwdQjBUXw5BXbvuY/kwbSDIwNkyQBWsjYt+sxweSGlhml4ypyig44fwUvH3Nz284VJGMCFI2fKDywtYjC7yoNWTieFSdblHeACXb1mW4IaWHUDz6KrQPifngfbmXsIO0YyhRHQ==",
            "supplies": "X4euZXvbGqyJuiSdDU0VBkB/noCFGmDPSaI3fV6twV02B1YyVdulqgnAWcbbj3M7/g5Im+tYH+BzHIScFGelOm3Vf1zVFIyAgOGVneiJ56zCjoZDw0TqvoLfhW5TFj76DX+PiIBQPnH/cGO6cJP81zMorNVcPZ6Pqc/NCokK4chq2uc2+MseP5wa/5gV9cfpnmjtShE7v31dBOIN5+UKqR4RzXgWjgZ0LufiawGTwQfk6pflarCjKDwonQ+0NP2hU8mfTJ5hgT/Hk/FxYEvbkriPuj2wF08TBjJoqY8Udga6C5QlFaEsyt0DZxNMT8/nJZPNKhaqPZgSFrJWjyN6EQ==",
            "lat": "HzR5QDgQ+aZTAALKKM68+bjh9LryS6emLdR3h02mTW46SYT0ADJrycvQYjfS+V0c4rlLejU/Kj6GeI6Gc42O+EvguLAOos3TZDn82VrfToRUWiVKNK98O3JQg64vhGzKruad6ClCQ8NS/bglh/3q+azMAcItl43ffAsvjdbLh+MM2TeLE/LzlsaEQYFWez+Z7oYnKqUtYU9IXsi0TCqOaLTZnwiHBY2E1MajGix8dh+tC+fsQUQI6+F7HRMcWk5duNEdGOcBXnXtHX6W75m0vVFHidEOsEhtXIUtfetB6THqCK2+sZJECqrmzNwMX66XGVUquoxn3h99r9cqkuG0WQ==",
            "lng": "W1EcmQBkPAcja3QHBalW6GTsB6oTSZTwdrqz75O5I9TMF0tYiuPaJw8QUfuWC0+bXVLsbjZtw99nj99fFosrATmwr9cNgnYfoUILESsXlvoDK0TuLNqcp2EplR4IpXLx3esTjLyMwmiUaaoOTK2ag3b4PSsrG7PkZTzcIdTjOGjjIroLZCiDWIkOWHt6/1X++enh63PeQMS0JQMTdIqASj/7VthX6zcRoK0CzqQPYEfwL8Nv6NarKfZ7RN+Uf6qk0sRMP+T4qDjfa8pWW5Atv88KgcnXUD6tSvj7ZfUrpq9JLu3eouebCtFsBcNQ6l8P8GGMXit5ly27/ZlS8CIvIw==",
            "duration": "hZ25oTRzg9JZNnM/VqWgL4OApuHEOjQTxWD5E1XIWwc1by0xNuc07K7LbtrFoxH+QoXoPYTybyAuBShcOavardzCcPPZJ4QdaCZbdq+NIUrLWNfbtoXJ6sk5IiCXSZgANReUAy0w3Yg/vrlnR9/gfbsidnmbpRujPAvQ5PX94baJLm97ZqJyx7DfaqnTj9RMVWtoTNyInqb6Vp3HZXWFXMS5WWVQpIBVnDXrx0HyyNvWxLPkK3fqa5qnhJwzERpMAM0J03G/Cxm31om8YhQeZ9LwJiVlG00QqdFPevquq67QaCDL4359qtFm6sR8zC3/3uqEQTi+TqHxj7TzAxh7DQ==",
            "markers": [
                {
                    "lat": "LoggTgAfnVlbQyyQ6v5eHIcx7+AaN/QzI0Ip5Uuln97exunAIMxkrGqBYBl2aTHXeG/MMXc/Q9lVSPv47xfj0aUHJwEtjwC6uhrAwY1LjM7d40evL8K3JYr8TcTLWSzjfvhSbMQVZqQyY/eR9KHq4o/Lmb2J6ddT2J3ojzEfir4q67cTRwnaKRlVdNV+B16EFiIfxecP/QPzInDxWc4EvVsJObKH1YRHcTEC3gGjV0xU0gVpTNCfXaPoxdaKQZ5dmyc/bHT2qrl3zeD6KeZxhnhyLeW0cRKOD9RR5eO2PVitSCxugnORP1hKO0JSTfLlIVbi9f7aOMup2n7/+TCN/w==",
                    "lng": "izuBk6ypwWKGq+k+GIyB2OqvPEnoSS+SeNPb+NJCnzuIESDCJw1+ko0e/ktHEVM2RnVNIXP0mFkgW82F6ca1wVwh+9qd1B96fA3Fibs37ZjE6awH0CeeQtt+tGslJ0pd3qYzLPRtI5ZuXL4uhjHeoA2gTP94ml+5t7Ysc4o382oszSTpfJEZQyPS4wi19TVsu+dQXAh8ByWIf+WYe1hHNByE6jF7joYapWaa xQ1ACbcK7NgzTx+Jg7FLsTveNlw611VATii0wD1eE9HdAybqsglQQU8sb9yN4fo7m2c1eJiPeflDbYFBo+k9mS8PwRmHrIhU91nbeVx5eaqDtQe3Og==",
                    "title": "HL+tJhK8cwojTS7VdPsZ7V+jcWHbFuXlbhP6V6RZ5H2Yz4wf0t5djjrQGEsKUBE3nPcwaYzRVePM7YiXdr5ME2cyFRchGDCaotrd8Dgjcmc05WnlmsMMSL+q2/37mDhcRHycqMUtL+TSWNffSCMBH9mIGashz+87l+TNjgHZKXaHEi9oL8NS7jldDSMlPrHblUe42Fwo6bN3XhMGUbdxNsIkvF6zgyP71vNq7yGc4+4e/VnTAZ/12WlXYZVc9nzaX6er2V+2cJ74ESe0q87eTOIztqMbMgS6MSoYDi7Y3s6DqGuzCf6YmVh1OKeuwh+89ngAVQa7yIwgk8QM8R0Q/w==",
                    "description": "id09yvbQshTGRaIFBs4pILeBgRyrAN1GB6FwfPIPJE9xFNjS3KLuErybIEbOvBN1J+G9Snv2AHAzlOJHG1bb4MCXOY46vAYc2Wiu6kwk52klcyYLbQbDtoSmZorpmhp5guvbipIgRDVGDYUfUFEZ6NJI CYaxr1ozsTCdzDOxHYte3DHi6jxQyUasYX482AmJ7ZVhymvTUYBk6vV0nZn44t3fG0GrGitqYNGd/qfIRd5vYUD26RRED8D3qEvmQ3s9xMnCnIS98HHWJ0NdoCg/O4Cc/E3+S/9qjqrvqe0SCHuPHOfHFm8ttQsrD7nYy61RBDWrhGEgK+WsuD9vW4zsmQ=="
                },
                {
                    "lat": "kJtC5SMUqvliwpx0NL69knR8+PRe/wWvTLiqaz4kLUB7RtjILcJB3GuruGarcRJCYNeQNpPs8PKo0dbLt2nTzWdVC+5uKKIMlmHc7GxthXo/YgS30d/TVdKqLnT1td9MiadpxoHgf+Xl7q/rkIV7dS4RmTmncS3mIAfE0CZkQsDiUVrvaSPyb+KLKPoycB0nmKo60A2i6Otnaqcr0Cc6tlaMZOy+pk3+aypgFkl+Z0NZ0PC4UQN/c6joVCoMSd7QG8c+IfVCFBriy8af6q2uDmfhiPnXowCikg1eLVeJgWOAeR8an9HUt9wdkaje/Zt4BwOMV1MqrTOTAH9WrDrH5g==",
                    "lng": "Jg77nq5I33ZBKUpWoEU6RRmxRDaZWNf5iTynMsk1Cyt8PODvFsDHXojSMsWVt44Fx1NeYttfDl1skOoGCmgsQ0l/F9vr/OyqF+kBam+dk/oFtACx9cF2fsqSQOpXR/Gzm1t0U2nn0VVK4frbNoJLB9SFan6wJfGEP4wlSov0wh3nl0l4w9kYsvLeNHUT/dtfaXa9Vs1M/C9zcNSmONMGUKrGvduDLgltwFi998T4jeD4FggkSfSlMWEogz9JMfWe3cKXjC0vgex0APUZwSajm+aS3JxuskbxSJjczdMW+guWWG9vGE32vXBDeBqwgXNJrO+Hh5TXCiOjr2HI/ryvVQ==",
                    "title": "g7VqbSt89XImdD0rZfLEni+CKtzDvcBnUMpIhvG9GD2de/th0yMd1AjrH3XLH9H3ex9AFp4zbfJBb7Mhv6P4FCeJH660D5R24F4jsrCwcEbsOsuN6mvo9R5afVi/gu4RzDnfXAOPQQtunC1zacho3aNuPf8HcfvZRTUaGIkar1zrn3xzl3vb5NQkyiAsnCX1ORt0CjWXF3G1nZWtkMFQAqhwekLvoyBnB66Xmc3lr/aHZVOheoyBGRRAlOy8cVJS8enOuJSoFzta5EPaN88j6LzaJEu6gL0wiLY4IOv719WCqBS5G0xHKrD0McNfX5Gg6GuN373Njrstfu9KxcRf5g==",
                    "description": "WlzJrlSb7XEQzrG5t4au0i8VsT+BGxizvCJkqQgL8zF26SXkZr7tkTp69ARQZIA8dVtfpLphSw3Tw+Wrh51FHCWhbY0tF69QJ+jC2sDLGJjSBYTz9jgYgiBI3cVXRzXDJ0X5LCM4l70Wu7xXLN429moWnpiOMRMzXdQ6qISjC1A/YZ9rp+yD5q19F8FEYaqdDjPJvgO4nZ0VeVaS/ZmVNf9CkuAWA33uk+MrqY7hEIqXOtwM/iQMowNa9LKThyWLwMVZY4nGgdxGdpnNB/hxP/D4Ns7okN9Cq8QohEVxhpYPxvoipOLxIgoUX/aqao2Eq32W70pC4ByovZ6nEwAQIg=="
                }
            ],
            "traveledPath": [],
            "completed": False,
            "inProgress": False
        }

        self.updated_user_data = {
            "uid": "XW+fFNjMyZkFMK6ye9LT8VpS477Clav4ZeA2PVFDOOCCzR68qGnWznVA44HY0HKAVtgcYhPtNwJqF3YssseVGM92II4PJD6QU+kp2b9HowVjoHB4P83Nfe5BnBjOb8gt2HOe/KTy6upjYxsvVUmZHf76wCQhmNS7zU7sf2Dlxk5wRSXv9ZpUil+oaa2Go9Mzz3183FWwnu3B2ZFTUv4iRRU6P/K3NayuObFjmbDC1Q6UcdUw/C15opNwioWTpN3AFXh8y153WJF5YHlbbKeER/W8HKENy/GmzbYK+W6HtK45PdwHPuRYpiRBQTSnVetXEAVwOfYpxadpCzHjcq6xYg==",
            "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApcGSi8vWxK89sgcwBv2licRE9KZR5PiEst7qpsn0hmmrDGvteoKKdHAHD/vmNFo3mLzMWGU6/2Tqryo4aL/FOwbrqWa58iKde2TFT5XIYj3pumC0JUdFqtGeNcZsOxtzcxQT25w/qwCIbo05ESBxmX+r3aJVT7/ylnXrzaePY+Fh6IAgx4+dr9wBgckBKTwR+omkl2yAPFcxYgLeWQuLOdzyb2U2YWHS5EzKX6Hk0qmmufKkB6lSwVf0PLBb7Kz27y/pX0x39l6jXeTdyEvNrhGE/j487a3HPKgIE8lFUsvjYe6pAV/wS/I5EH3/iDZM+A8L1y+nIykDKj+mQ5aQywIDAQAB",
            "fName": "Q7Qkwu3qN1+UXqheG+FBeDGzmV6CARX+NG4uOf+T+mZqtvivzvt/6LgDs8jPF5sRdxWSHi5pb4cGJnyUMCtDfiKIcR7ybesNGtvH1gKaLRcmgBs3Ttmx5fv0cXPtKHu/Hi1+4Cjt//KLXPKcNYL29RndtROrR1xLKrS1wPJNpX0E/sjhryUTvI/sDQQmg3TvZCXHcBDqozXxjP8rCstTBZrjYpvTzvLRS39wPFKqO1Nu912F1C8bL/hnRtD97s/O25rZRIVSfPuK+p2CKfCkdNosIRdc4gv78X2NcDj19A/ZPAjisOusruS8tyaaxTWDT5Rd9aRONIP9MFyPTeqJWw==",
            "lName": "OxTbceTDdeEHYnJoQKfHKuuarc2vhshN5YdNgcMTGy3RoHLbAxmZpISU9tAujfUu1heRaO7AckMy5BHr6NcPi2dI/dt4ilNYUXmRvJTvEPZTyG6ucibM0sj7Fy8VKWE9Xlt1RetsSrs+C+t/PniePOjIfszgC+USYM+XT3rMGA8nyM3s4rWxuFySLx+nqHfMsiy+GbBeJp8wLN/+SdJMM68qf2rHLa+Da9CAJovkrmcAfZoNS3962Jq0N2M/wK+lpMhBFlQWvu6sHcGz9EXkyLKvRPGhIPpGQtQQZrIkBuC0XcyrKbYCVOdzZuIKzctQ+GeNAIC+Nyg1weW4BlwQBA==",
            "emergencyContacts": [
                {
                    "phoneNumber": "jgf5hA3k4KS3qsZJ767u7uz4KrtrEOl5ELIFkGsLMpdY9xv1k6OFbzq2MpwSuQ0J5xtBeoYphDcq+SmFj88fJRhBYIl5kiJy92arWHI3IkhTUTNDzXpRw2xD3/+dNDusu7c4yIy3MC7NSf/HMADfaeDyaAZ7J4mdWtdg5DqB59kCFxT6G23Qvlt9+FwfjjpSSm0FRE8H6dDEhv15nz8kckrusfpe7FrWnwhPHD61KeBukDXSTnYy+j/8w5ruyb2u8z9tuSVBJSwIhhAmx+lDpuSnFJ70iHTFeHe2Sl7k6+Od/uHxqR13ACFtdHNBu0LERC1a2CBl14lsm2S+RwNydw==",
                    "email": "I0JChXDaWXILunVUlZ7J8hc2A7WmLt4OgDf38cnChxVEvzdYPjpnxZSQ/WdBAKuVn1PB+nuurIGBNCazTnPZBB8brlpyqI+P8Q2QUXDURwYjMya0l0nubSCKN120h4vuIgSPdSOoQEU+ec5Vx62Wysx7xRw5lzsSb3pa+qhNP+dFEYeIuYkDQoeoxgM/avwe0jRAOwXYS/qfHErYMVvviJ70berF+SFMtadLv8+/vV5Iy1Rx9DCFRnZiNiNouiftyVdN4VDOebyLTo//MIMjbUPLg9lLbEtYpHj/QQ8dp3Jc2VgfQKQCN2lvnFfktUvxbzfN7iP3gULRJCmtGLYvrw=="
                }
            ]
        }

        self.start_hike_data = {
            "pid": 1,
            "uid": "Go0CececmHPzutgRYYDLqqJW/ihI9zUwDOdvm+m89ArX4mWcJGV2c9fwzJZNZ0zDBW9sHdNRqBHG51HTuAYYQ7EnzCwxCU4tWAkGK+KVARsSdJTHHV0lhXhFHfvLHhYlVQqzzewmX/pvP7FPTcf6vhB3kSar4gNpKJ+Mgba0lA3dorDknXQTroFUh9Xx5FEyNZMkq/SYBVeEqtTWgvx8o4UT3y1GiyTjqsD0dYzLJEmbU9Fada/oIb6B5Ib2KR0cYS0QlMudkHHQcLhDeY9uqMgEX0GDJTZ5b7fW6JDMJ+TU9GfefZGgTsm3XZ04EFME3bWh419AR1y81R+i2JMSfg==",
            "name": "BcnovVRcj520qbeoqohbHj9CDYafL+g9Az0OEDyOvqcjClwDfsf8Zf16Gd7EMFysw5yabsncN/oIfL27plCihYX7C7MgRHAggSjrVLynLmWQOelHGYbub/Xu+6Kc5MDfUothTM46LHlv6/CLWTesFlK4/oXsMts+kvtWQCI0cAHjDq39aD94WOqeh/9Yg52m9ZBCbzFKbySaZEQNgwdQjBUXw5BXbvuY/kwbSDIwNkyQBWsjYt+sxweSGlhml4ypyig44fwUvH3Nz284VJGMCFI2fKDywtYjC7yoNWTieFSdblHeACXb1mW4IaWHUDz6KrQPifngfbmXsIO0YyhRHQ==",
            "supplies": "X4euZXvbGqyJuiSdDU0VBkB/noCFGmDPSaI3fV6twV02B1YyVdulqgnAWcbbj3M7/g5Im+tYH+BzHIScFGelOm3Vf1zVFIyAgOGVneiJ56zCjoZDw0TqvoLfhW5TFj76DX+PiIBQPnH/cGO6cJP81zMorNVcPZ6Pqc/NCokK4chq2uc2+MseP5wa/5gV9cfpnmjtShE7v31dBOIN5+UKqR4RzXgWjgZ0LufiawGTwQfk6pflarCjKDwonQ+0NP2hU8mfTJ5hgT/Hk/FxYEvbkriPuj2wF08TBjJoqY8Udga6C5QlFaEsyt0DZxNMT8/nJZPNKhaqPZgSFrJWjyN6EQ==",
            "lat": "HzR5QDgQ+aZTAALKKM68+bjh9LryS6emLdR3h02mTW46SYT0ADJrycvQYjfS+V0c4rlLejU/Kj6GeI6Gc42O+EvguLAOos3TZDn82VrfToRUWiVKNK98O3JQg64vhGzKruad6ClCQ8NS/bglh/3q+azMAcItl43ffAsvjdbLh+MM2TeLE/LzlsaEQYFWez+Z7oYnKqUtYU9IXsi0TCqOaLTZnwiHBY2E1MajGix8dh+tC+fsQUQI6+F7HRMcWk5duNEdGOcBXnXtHX6W75m0vVFHidEOsEhtXIUtfetB6THqCK2+sZJECqrmzNwMX66XGVUquoxn3h99r9cqkuG0WQ==",
            "lng": "W1EcmQBkPAcja3QHBalW6GTsB6oTSZTwdrqz75O5I9TMF0tYiuPaJw8QUfuWC0+bXVLsbjZtw99nj99fFosrATmwr9cNgnYfoUILESsXlvoDK0TuLNqcp2EplR4IpXLx3esTjLyMwmiUaaoOTK2ag3b4PSsrG7PkZTzcIdTjOGjjIroLZCiDWIkOWHt6/1X++enh63PeQMS0JQMTdIqASj/7VthX6zcRoK0CzqQPYEfwL8Nv6NarKfZ7RN+Uf6qk0sRMP+T4qDjfa8pWW5Atv88KgcnXUD6tSvj7ZfUrpq9JLu3eouebCtFsBcNQ6l8P8GGMXit5ly27/ZlS8CIvIw==",
            "duration": "Ur2Y498sz82/OQREB6uHopZCEKt4f9lkpL6PXP7V3HUTZD8IUDGSc9axvvCQ0fZtJxTvWKnAnjrux1LnHbE8RSaiIP8I3ipFE6RDztd/jTAv03KLHr0WOjdOoYDrNiH5GakkvmxV0/lpBVtbQKKPY4tyEBIU+ZtfSJ7iBtN9AXQ+9W9prE+T7DrHHuxJLX1Cra5xjs3eIfcKMayzuFEWtsj5RKqkpM1B9OgS1EMUbv4DkGA05Zyd14Lx3pPaWyHqr87ftg+DOhs5FUuILQ101aolmrMy3Ylg892odPutPrdx1XvCIQE4s65NNOf+gQLNzFv25mm252Ua1/xTgzpECQ==",
            "markers": [
                {
                    "lat": "LoggTgAfnVlbQyyQ6v5eHIcx7+AaN/QzI0Ip5Uuln97exunAIMxkrGqBYBl2aTHXeG/MMXc/Q9lVSPv47xfj0aUHJwEtjwC6uhrAwY1LjM7d40evL8K3JYr8TcTLWSzjfvhSbMQVZqQyY/eR9KHq4o/Lmb2J6ddT2J3ojzEfir4q67cTRwnaKRlVdNV+B16EFiIfxecP/QPzInDxWc4EvVsJObKH1YRHcTEC3gGjV0xU0gVpTNCfXaPoxdaKQZ5dmyc/bHT2qrl3zeD6KeZxhnhyLeW0cRKOD9RR5eO2PVitSCxugnORP1hKO0JSTfLlIVbi9f7aOMup2n7/+TCN/w==",
                    "lng": "izuBk6ypwWKGq+k+GIyB2OqvPEnoSS+SeNPb+NJCnzuIESDCJw1+ko0e/ktHEVM2RnVNIXP0mFkgW82F6ca1wVwh+9qd1B96fA3Fibs37ZjE6awH0CeeQtt+tGslJ0pd3qYzLPRtI5ZuXL4uhjHeoA2gTP94ml+5t7Ysc4o382oszSTpfJEZQyPS4wi19TVsu+dQXAh8ByWIf+WYe1hHNByE6jF7joYapWaa xQ1ACbcK7NgzTx+Jg7FLsTveNlw611VATii0wD1eE9HdAybqsglQQU8sb9yN4fo7m2c1eJiPeflDbYFBo+k9mS8PwRmHrIhU91nbeVx5eaqDtQe3Og==",
                    "title": "HL+tJhK8cwojTS7VdPsZ7V+jcWHbFuXlbhP6V6RZ5H2Yz4wf0t5djjrQGEsKUBE3nPcwaYzRVePM7YiXdr5ME2cyFRchGDCaotrd8Dgjcmc05WnlmsMMSL+q2/37mDhcRHycqMUtL+TSWNffSCMBH9mIGashz+87l+TNjgHZKXaHEi9oL8NS7jldDSMlPrHblUe42Fwo6bN3XhMGUbdxNsIkvF6zgyP71vNq7yGc4+4e/VnTAZ/12WlXYZVc9nzaX6er2V+2cJ74ESe0q87eTOIztqMbMgS6MSoYDi7Y3s6DqGuzCf6YmVh1OKeuwh+89ngAVQa7yIwgk8QM8R0Q/w==",
                    "description": "id09yvbQshTGRaIFBs4pILeBgRyrAN1GB6FwfPIPJE9xFNjS3KLuErybIEbOvBN1J+G9Snv2AHAzlOJHG1bb4MCXOY46vAYc2Wiu6kwk52klcyYLbQbDtoSmZorpmhp5guvbipIgRDVGDYUfUFEZ6NJI CYaxr1ozsTCdzDOxHYte3DHi6jxQyUasYX482AmJ7ZVhymvTUYBk6vV0nZn44t3fG0GrGitqYNGd/qfIRd5vYUD26RRED8D3qEvmQ3s9xMnCnIS98HHWJ0NdoCg/O4Cc/E3+S/9qjqrvqe0SCHuPHOfHFm8ttQsrD7nYy61RBDWrhGEgK+WsuD9vW4zsmQ=="
                },
                {
                    "lat": "kJtC5SMUqvliwpx0NL69knR8+PRe/wWvTLiqaz4kLUB7RtjILcJB3GuruGarcRJCYNeQNpPs8PKo0dbLt2nTzWdVC+5uKKIMlmHc7GxthXo/YgS30d/TVdKqLnT1td9MiadpxoHgf+Xl7q/rkIV7dS4RmTmncS3mIAfE0CZkQsDiUVrvaSPyb+KLKPoycB0nmKo60A2i6Otnaqcr0Cc6tlaMZOy+pk3+aypgFkl+Z0NZ0PC4UQN/c6joVCoMSd7QG8c+IfVCFBriy8af6q2uDmfhiPnXowCikg1eLVeJgWOAeR8an9HUt9wdkaje/Zt4BwOMV1MqrTOTAH9WrDrH5g==",
                    "lng": "Jg77nq5I33ZBKUpWoEU6RRmxRDaZWNf5iTynMsk1Cyt8PODvFsDHXojSMsWVt44Fx1NeYttfDl1skOoGCmgsQ0l/F9vr/OyqF+kBam+dk/oFtACx9cF2fsqSQOpXR/Gzm1t0U2nn0VVK4frbNoJLB9SFan6wJfGEP4wlSov0wh3nl0l4w9kYsvLeNHUT/dtfaXa9Vs1M/C9zcNSmONMGUKrGvduDLgltwFi998T4jeD4FggkSfSlMWEogz9JMfWe3cKXjC0vgex0APUZwSajm+aS3JxuskbxSJjczdMW+guWWG9vGE32vXBDeBqwgXNJrO+Hh5TXCiOjr2HI/ryvVQ==",
                    "title": "g7VqbSt89XImdD0rZfLEni+CKtzDvcBnUMpIhvG9GD2de/th0yMd1AjrH3XLH9H3ex9AFp4zbfJBb7Mhv6P4FCeJH660D5R24F4jsrCwcEbsOsuN6mvo9R5afVi/gu4RzDnfXAOPQQtunC1zacho3aNuPf8HcfvZRTUaGIkar1zrn3xzl3vb5NQkyiAsnCX1ORt0CjWXF3G1nZWtkMFQAqhwekLvoyBnB66Xmc3lr/aHZVOheoyBGRRAlOy8cVJS8enOuJSoFzta5EPaN88j6LzaJEu6gL0wiLY4IOv719WCqBS5G0xHKrD0McNfX5Gg6GuN373Njrstfu9KxcRf5g==",
                    "description": "WlzJrlSb7XEQzrG5t4au0i8VsT+BGxizvCJkqQgL8zF26SXkZr7tkTp69ARQZIA8dVtfpLphSw3Tw+Wrh51FHCWhbY0tF69QJ+jC2sDLGJjSBYTz9jgYgiBI3cVXRzXDJ0X5LCM4l70Wu7xXLN429moWnpiOMRMzXdQ6qISjC1A/YZ9rp+yD5q19F8FEYaqdDjPJvgO4nZ0VeVaS/ZmVNf9CkuAWA33uk+MrqY7hEIqXOtwM/iQMowNa9LKThyWLwMVZY4nGgdxGdpnNB/hxP/D4Ns7okN9Cq8QohEVxhpYPxvoipOLxIgoUX/aqao2Eq32W70pC4ByovZ6nEwAQIg=="
                }
            ],
            "traveledPath": [],
            "completed": False,
            "inProgress": False
        }

    @pytest.mark.order(1)
    def test_create_user(self):
        # Test user creation
        response = self.app.post('/users', json=self.user_data)
        self.assertEqual(response.status_code, 201)
        self.assertIn(b"User created successfully", response.data)

    @pytest.mark.order(2)
    def test_create_hike(self):
        # Test hike creation
        response = self.app.post('/hikes', json=self.hike_data)
        self.assertEqual(response.status_code, 201)
        self.assertIn(b"Hike processed successfully", response.data)

    @pytest.mark.order(3)
    def test_get_hikes_for_user(self):
        # Fetch hikes for a user
        response = self.app.get('/hikes?uid=7yYwXBrj0QNDQk0vB6OyMrSqThS2')
        self.assertEqual(response.status_code, 200)

    @pytest.mark.order(4)
    def test_update_hike(self):
        # Test hike update
        updated_hike_data = self.hike_data.copy()
        updated_hike_data['name'] = "mWjhT/l7hE9oltVjw50GsQsSe6z9DssHRjm513zzDsipVUJ563oF4Qf0v/4tst0QHkAhOudN/BvOQ/7ynaP9oXYJFwqIcgc9D5nqZmfEousFEGRyI7wM9iOf0Ifb29lq+Gs+3XI4DG5+riy5Yf36r5Xq3EoaQkYc4ZQAgb9HO4arDrA1TCzE5Vg0HGQRt3rwTYVE9kPVTZsxAHgADiuFU5LNA5uncdMfRYKic0l92Ay0cWGmykFNKv6AfF47jkdsxteDqekQ6PukxejeuX9hQoOEFySYSqSmDHl+MVilgMLsv1VjrD6nH4kdJUxAIlquv5rznEkdtheXQPmHx3vkRQ=="
        response = self.app.put('/hikes', json=updated_hike_data)
        self.assertEqual(response.status_code, 200)

    @pytest.mark.order(5)
    def test_start_hike(self):
        start_hike_data = self.start_hike_data.copy()
        response = self.app.put('/hikes/start', json=start_hike_data)
        self.assertEqual(response.status_code, 200)

    @pytest.mark.order(6)
    def test_delete_hike(self):
        # Test hike deletion
        response = self.app.delete('/hikes/1')
        self.assertEqual(response.status_code, 200)
        self.assertIn(b"Hike with pid 1 deleted successfully", response.data)

    @pytest.mark.order(7)
    def test_get_user(self):
        # Fetch user details
        response = self.app.get('/users?uid=7yYwXBrj0QNDQk0vB6OyMrSqThS2')
        self.assertEqual(response.status_code, 200)

    @pytest.mark.order(8)
    def test_update_user(self):
        # Update user details
        updated_user_data = self.updated_user_data.copy()
        response = self.app.put('/users', json=updated_user_data)
        self.assertEqual(response.status_code, 200)

    @pytest.mark.order(9)
    def test_serve_map_file(self):
        response = self.app.get('/maps/map_a9cf38be-0821-49ec-8ee0-6887e4fad810.html')
        self.assertEqual(response.status_code, 200)

    @pytest.mark.order(10)
    def test_serve_nonexistent_map_file(self):
        response = self.app.get('/maps/blah.html')
        self.assertEqual(response.status_code, 404)
