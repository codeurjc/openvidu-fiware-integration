<template>
    <div class="container">
        <div id="video-container"></div>
        <q-card>
            <q-card-section class="title">
                <h6><i class="fas fa-video"></i> WebRtc camera <i class="fas fa-video"></i></h6>
            </q-card-section>
            <q-card-section>
                <q-input outlined v-model="ovData.cameraUuid" label="Uuid" disabled></q-input>
                <q-input outlined v-model="ovData.description" label="Description" disabled class="q-mt-md"></q-input>
                <div class="errors" v-if="error !== ''">{{ error }}</div>
            </q-card-section>
            <q-card-actions class="actions">
                <q-btn color="negative" label="Remove camera" @click="removeCamera"></q-btn>
            </q-card-actions>
        </q-card>
    </div>
</template>

<script>
    module.exports = {
        data() {
            return {
                OV: null,
                session: null,
                publisher: null,
                error: ""
            };
        },
        computed: {
            ...Vuex.mapState({
                ovData: "data"
            })
        },
        methods: {
            async removeCamera() {
                let result = await doDelete(apiPath + "/camera/" + this.ovData.cameraUuid, {});

                if (result.isOk) {
                    this.$store.commit("changeView", "main");
                    this.$store.commit("changeData", null);
                } else {
                    console.error(result);
                }
            },
            publish() {
                if (this.publisher !== null) {
                    return;
                }

                console.log("publishing");

                this.publisher = this.OV.initPublisher("video-container", {
                    audioSource: undefined, // The source of audio. If undefined default microphone
                    videoSource: undefined, // The source of video. If undefined default webcam
                    publishAudio: true,  	// Whether you want to start publishing with your audio unmuted or not
                    publishVideo: true,  	// Whether you want to start publishing with your video enabled or not
                    resolution: "640x480",  // The resolution of your video
                    frameRate: 30,			// The frame rate of your video
                    insertMode: "APPEND",	// How the video is inserted in the target element 'video-container'
                    mirror: false       	// Whether to mirror your local video or not
                });

                this.publisher.on("videoElementCreated", (event) => {
                    document.querySelector("#video-container video").srcObject = event.element.srcObject;
                    event.element.setAttribute("muted", true); // Mute local video
                });

                this.session.publish(this.publisher);

            },
            unpublish() {
                if (this.publisher === null) {
                    return;
                }

                console.log("unpublishing");

                this.session.unpublish(this.publisher);
                this.publisher = null;
            }
        },
        mounted() {
            window.onbeforeunload = () => {
                this.session.disconnect();
                this.session = null;
                this.publisher = null;
                this.OV = null;
            };

            this.OV = new OpenVidu();
            this.session = this.OV.initSession();

            this.session.on("streamCreated", (event) => {
                this.session.subscribe(event.stream, "video-container");
            });

            this.session.on("switch-state", (event) => {
                let value = event.data === "true";

                if (value) {
                    this.publish();
                    console.log("a");
                } else {
                    this.unpublish();
                    console.log("v");
                }
            });

            this.session.connect(this.ovData.ovtoken, {
                publisher: true,
                amITheCamera: "yes-I-am"
            })
            .then(() => {
                this.publish();
            }).catch(error => {
                console.warn("There was an error connecting to the session:", error.code, error.message);
            });
        },
        beforeDestroy() {
            window.onbeforeunload();
            window.onbeforeunload = null;
        }
    };
</script>

<style scoped>
    .container {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        width: 100%;
        height: 100%;
    }

    .title {
        text-align: center;
    }

    .actions {
        display: flex;
        justify-content: center;
    }
</style>
