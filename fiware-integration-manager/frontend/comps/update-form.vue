<template>
    <q-card>
        <q-card-section>
            <div class="text-h6 text-center"><i class="fas fa-filter"></i> Update camera
                <i class="fas fa-filter"></i>
            </div>
        </q-card-section>
        <q-card-section class="button-container">
            <q-input outlined v-model="descriptionModel" label="Description" dense class="q-mb-md"></q-input>
            <q-select outlined
                      v-model="filter"
                      :options="filterOptions"
                      label="Filter"
                      dense
                      class="q-mb-md"></q-select>
            <q-select outlined v-model="events" :options="eventOptions" label="Events" multiple dense></q-select>
            <q-toggle v-model="active"
                      checked-icon="fas fa-check"
                      color="red"
                      label="Is enabled?"
                      active
                      unchecked-icon="fas fa-times"></q-toggle>
        </q-card-section>
        <q-card-actions class="actions">
            <q-btn color="primary" label="Update" @click="update"></q-btn>
        </q-card-actions>
        <q-card-actions class="actions2">
            <q-btn color="negative" label="Remove camera" @click="remove"></q-btn>
            <q-btn color="primary" icon="fas fa-sync" label="Refresh" @click="refresh"></q-btn>
        </q-card-actions>
    </q-card>
</template>

<script>
    module.exports = {
        data() {
            return {
                filter: "None",
                filterOptions: ["None", "platedetector.PlateDetectorFilter"],
                events: [],
                availableEvents: {
                    "None": [],
                    "platedetector.PlateDetectorFilter": ["PlateDetected"]
                },
                description: null,
                active: null
            };
        },
        computed: {
            eventOptions() {
                return this.availableEvents[this.filter];
            },
            activateButtonText() {
                return this.selectedCamera.deviceState === "active" ? "Deactivate" : "Activate";
            },
            descriptionModel: {
                get() {
                    return this.description !== null ? this.description : this.selectedCamera.description;
                },
                set(value) {
                    this.description = value;
                }
            },

            ...Vuex.mapState(["selectedCamera"])
        },
        methods: {
            async update() {
                if (this.filter !== this.filterOptions[0] && this.events.length === 0) {
                    this.$q.dialog({
                        title: "ERROR",
                        message: "The events cannot be empty."
                    }).onOk(() => {
                    });
                    return;
                }

                let response = await doPost(apiUrl + "/camera/" + this.selectedCamera.id, {
                    description: this.descriptionModel,
                    filter: this.filter === this.filterOptions[0] ? null : this.filter,
                    events: this.events,
                    active: this.active
                });

                if (!response.isOk) {
                    console.error(response);
                    return;
                }

                this.$emit("refresh");
            },
            async refresh() {
                let response = await doGet(orionUrl + "/v2/entities/" + this.selectedCamera.id + "?options=keyValues");

                if (!response.isOk) {
                    console.log(response);
                    return;
                }

                let camera = response.response;
                let filterEvents = getFilterAndEventsFromCamera(this.selectedCamera);
                let filter = filterEvents.filter;
                const events = filterEvents.events;

                if (!filter) {
                    this.filter = this.filterOptions[0];
                } else {
                    this.filter = filter;
                    Vue.nextTick(() => {
                        this.events.splice(0, this.events.length, ...events);
                    });
                }

                this.active = camera.deviceState === "active";

                console.log(this.events);
            },
            async remove() {
                let response = await doDelete(apiUrl + "/camera/" + this.selectedCamera.id);

                if (!response.isOk) {
                    console.log(response);
                    return;
                }

                this.$store.commit("changeSelectedCamera", null);
                this.$emit("refresh");
            }
        },
        watch: {
            filter() {
                this.events.splice(0, this.events.length);
            }
        },
        mounted() {
            this.refresh();
        }
    };
</script>

<style scoped>
    .actions {
        display: flex;
        justify-content: center;
    }

    .actions2 {
        display: flex;
        justify-content: space-between;
    }
</style>
