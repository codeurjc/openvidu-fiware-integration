<template>
    <div class="main">
        <q-table title="Available OpenVidu cameras"
                 :data="cameras"
                 :columns="columns"
                 row-key="name"
                 selection="single"
                 :selected.sync="selected"></q-table>
        <div class="actions">
            <q-btn color="primary" icon="fas fa-sync" label="Refresh" @click="refresh"></q-btn>
        </div>
    </div>
</template>

<script>
    module.exports = {
        data() {
            return {
                error: "",
                columns: [{
                    label: "Name",
                    field: "name",
                    sortable: true,
                    align: "left"
                }, {
                    label: "Description",
                    field: "description",
                    align: "left"
                }, {
                    label: "State",
                    field: "deviceState",
                    align: "left"
                }, {
                    label: "Filter",
                    field: "filter",
                    align: "left"
                }, {
                    label: "Events",
                    field: "eventsTxt",
                    align: "left"
                }]
            };
        },
        computed: {
            ...Vuex.mapState(["cameras", "selectedCamera"]),
            selected: {
                get() {
                    if (this.selectedCamera) {
                        return [this.selectedCamera];
                    }
                    return [];
                },
                set(value) {
                    if (value.length !== 0) {
                        apiUrl = value[0].ipAddress[1] + "/api/v1";
                        this.$store.commit("changeSelectedCamera", value[0]);
                    } else {
                        apiUrl = "<apiUrl unset>";
                        this.$store.commit("changeSelectedCamera", null);
                    }
                }
            }
        },
        methods: {
            async refresh() {
                let cameraType = "OVCamera";
                let response = await doGet(orionUrl + "/v2/entities?type=" + cameraType + "&options=keyValues");
                    console.log("Refresh response", response);

                if (!response.isOk) {
                    console.error(response);
                    return;
                }

                let cameras = response.response;
                console.log("Refresh cameras", cameras);
                cameras.forEach((it) => {
                    let filterEvents = getFilterAndEventsFromCamera(it);
                    let filter = filterEvents.filter;
                    const events = filterEvents.events;

                    it.filter = filter;
                    it.events = events;
                    it.eventsTxt = events.join(", ");
                });

                this.$store.commit("changeCameras", cameras);

                if (this.selectedCamera) {
                    let selectedId = this.selectedCamera.id;
                    this.selected = cameras.filter((cam) => cam.id === selectedId);
                } else {
                    this.selected = [];
                }
            }
        },
        mounted() {
            this.refresh();
        }
    };
</script>

<style scoped>
    .main {
        margin: auto;
    }

    .actions {
        display: flex;
        justify-content: center;
        margin-top: 20px;
    }
</style>
