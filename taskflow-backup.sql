--
-- PostgreSQL database dump
--

\restrict fQJzgpNeeOsZFKUNajHG7DaRxnQOW60583NiqggztpdiUo7W8HVtaSi5211x0fY

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: taskflow
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO taskflow;

--
-- Name: tasks; Type: TABLE; Schema: public; Owner: taskflow
--

CREATE TABLE public.tasks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'TODO'::character varying NOT NULL,
    due_date date,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.tasks OWNER TO taskflow;

--
-- Name: users; Type: TABLE; Schema: public; Owner: taskflow
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_users_email_lowercase CHECK (((email)::text = lower((email)::text)))
);


ALTER TABLE public.users OWNER TO taskflow;

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: taskflow
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create tasks table	SQL	V1__create_tasks_table.sql	1441936526	taskflow	2026-10-09 10:27:28.099873	48	t
2	2	add task indexes	SQL	V2__add_task_indexes.sql	-684897520	taskflow	2026-10-09 12:12:47.502195	86	t
3	3	create users table	SQL	V3__create_users_table.sql	45603930	taskflow	2026-10-11 16:18:40.721264	95	t
4	4	create refresh tokens table	SQL	V4__create_refresh_tokens_table.sql	0	taskflow	2026-10-12 12:24:38.057727	20	t
\.


--
-- Data for Name: tasks; Type: TABLE DATA; Schema: public; Owner: taskflow
--

COPY public.tasks (id, title, description, status, due_date, created_at, updated_at) FROM stdin;
fc589331-6b04-43a2-bdf5-aad9e38dac5b	Learn Spring	\N	TODO	2030-01-01	2026-10-09 08:46:58.795217	2026-10-09 08:46:58.795217
61770177-a246-4926-b49f-fc0fa95029ba	Going to church	\N	TODO	2030-02-02	2026-10-09 08:53:08.546951	2026-10-09 08:53:08.546951
4a6a2b12-7e4c-4c4e-91f1-d529be8dec79	Going to school	\N	TODO	2030-02-02	2026-10-09 09:40:55.287625	2026-10-09 09:40:55.287625
6944905a-aee2-4036-a50c-f32798f61fb3	Going to work	\N	TODO	2030-02-02	2026-10-09 09:41:02.018481	2026-10-09 09:41:02.018481
5656c70c-f189-4277-b403-91cdb038f8aa	Going to study	\N	TODO	2030-02-02	2026-10-09 09:41:07.320409	2026-10-09 09:41:07.320409
575fba1c-411e-4272-aadb-c610ad8c12aa	Going to gym	\N	TODO	2030-02-02	2026-10-09 09:41:13.121126	2026-10-09 09:41:13.121126
0bf22e28-fdf7-4eb3-a26c-94c8e4a309d7	Going to lessons	\N	TODO	2030-02-02	2026-10-09 09:41:19.001125	2026-10-09 09:41:19.001125
9cd6991a-4340-41a8-a164-e669efa18488	Going to home	\N	TODO	2030-02-02	2026-10-09 09:41:24.823259	2026-10-09 09:41:24.823259
918c4c34-2fde-4890-b3c0-ffe229d41f75	Going to the shop	\N	TODO	2030-02-02	2026-10-09 09:41:31.630116	2026-10-09 09:41:31.630116
ebdd0d66-56ee-435f-b859-337797d48148	Going to the market	\N	TODO	2030-02-02	2026-10-09 09:41:40.577447	2026-10-09 09:41:40.577447
27a5ded5-7f0f-47ef-b257-b22ee1c81e06	Going to the supermarket	\N	TODO	2030-02-02	2026-10-09 09:41:48.253757	2026-10-09 09:41:48.253757
ef8e280e-f6ca-4cd6-b2da-a65f763e426f	doing some shopping	\N	TODO	2030-02-02	2026-10-09 09:42:00.122203	2026-10-09 09:42:00.122203
51ee8a62-5d98-4bd1-b70e-f60898305b9e	doing some window	\N	TODO	2030-02-02	2026-10-09 09:42:07.06445	2026-10-09 09:42:07.06445
f6071caa-be72-447c-b8dc-e55e9628e684	working hard	\N	TODO	2030-02-02	2026-10-09 09:42:13.835573	2026-10-09 09:42:13.835573
4a0db322-4cdf-4563-99b8-8bfbc4a8c8b8	working slow	\N	TODO	2030-02-02	2026-10-09 09:42:19.782862	2026-10-09 09:42:19.782862
04060d39-122d-444a-96c9-225e5ec3bf37	excerciseing	\N	TODO	2030-02-02	2026-10-09 09:42:27.62017	2026-10-09 09:42:27.62017
898e6e30-6687-4460-88f4-058b07faf974	excerciseing alot	\N	TODO	2030-02-02	2026-10-09 09:42:32.345647	2026-10-09 09:42:32.345647
bc50eb54-bbd8-43b9-8e36-1fbc1bc0074d	today	\N	TODO	2030-02-02	2026-10-09 09:42:38.590465	2026-10-09 09:42:38.590465
9343646d-c43a-4c46-9d29-f6690d94c061	tomoorow	\N	TODO	2030-02-02	2026-10-09 09:42:46.295344	2026-10-09 09:42:46.29592
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: taskflow
--

COPY public.users (id, email, password_hash, role, enabled, created_at, updated_at) FROM stdin;
\.


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: taskflow
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: tasks tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: taskflow
--

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_pkey PRIMARY KEY (id);


--
-- Name: users uq_users_email; Type: CONSTRAINT; Schema: public; Owner: taskflow
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: taskflow
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: taskflow
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_tasks_created_at; Type: INDEX; Schema: public; Owner: taskflow
--

CREATE INDEX idx_tasks_created_at ON public.tasks USING btree (created_at DESC);


--
-- Name: idx_tasks_due_date; Type: INDEX; Schema: public; Owner: taskflow
--

CREATE INDEX idx_tasks_due_date ON public.tasks USING btree (due_date);


--
-- Name: idx_tasks_status; Type: INDEX; Schema: public; Owner: taskflow
--

CREATE INDEX idx_tasks_status ON public.tasks USING btree (status);


--
-- PostgreSQL database dump complete
--

\unrestrict fQJzgpNeeOsZFKUNajHG7DaRxnQOW60583NiqggztpdiUo7W8HVtaSi5211x0fY

