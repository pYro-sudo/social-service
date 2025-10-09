-- set client_encoding = 'UTF-8';
-- create database spring_microservice_course_db;
-- /c spring_microservice_course_db

-- create tablespace spring_microservice_course_ts location '/';

create schema if not exists gallery;
create table if not exists gallery.images (
    id serial primary key,
    url varchar(500) not null,
    description varchar(500) default '',
    uploadedAt date not null default now(),
    user_id serial not null,
    foreign key (user_id) references user_schema.users(id) on delete cascade
) /*tablespace spring_microservice_course_ts partition by range(id)*/;

-- create table if not exists gallery.images_p1
--     partition of gallery.images
--         for values from (0) to (500)
--     with (pages_per_range = 3, fastupdate = true)
--     tablespace spring_microservice_course_ts;
--
-- create table if not exists gallery.images_p2
--     partition of gallery.images
--         for values from (501) to (1000)
--     with (pages_per_range = 3, fastupdate = true)
--     tablespace spring_microservice_course_ts;
--
-- create index if not exists idx_gallery_images on gallery.images(id);
-- create index if not exists idx_gallery_images_p1 on gallery.images_p1(id);
-- create index if not exists idx_gallery_images_p2 on gallery.images_p2(id);

prepare gallery_images_insrt
    (varchar(500), varchar(500), date, serial) as
    insert into gallery.images
        (url, description, uploaded_at, user_id)
    values
        ($1, $2, $3, $4)
    on conflict do nothing;

create schema if not exists user_schema;
create table if not exists user_schema.users (
    id serial primary key,
    username varchar(20) not null unique,
    email varchar(50) not null default '',
    password varchar(20) not null,
    user_role varchar(20) not null default 'USER',
    enabled bool not null
) /*tablespace spring_microservice_course_ts partition by range(id)*/;

-- create table if not exists user_schema.users_p1
--     partition of user_schema.users
--         for values from (0) to (500)
--     with (pages_per_range = 3, fastupdate = true)
--     tablespace spring_microservice_course_ts;
--
-- create table if not exists user_schema.users_p2
--     partition of user_schema.users
--         for values from (501) to (1000)
--     with (pages_per_range = 3, fastupdate = true)
--     tablespace spring_microservice_course_ts;
--
-- create index if not exists idx_user_schema_users on user_schema.users(id);
-- create index if not exists idx_user_schema_users_p1 on user_schema.users_p1(id);
-- create index if not exists idx_user_schema_users_p2 on user_schema.users_p2(id);

prepare user_schema_users_insrt
    (varchar(20), varchar(50), varchar(20), varchar, varchar(20)) as
    insert into user_schema.users
        (username, email, password, salt, user_role)
    values ($1, $2, $3, $4, $5)
    on conflict do nothing;

create table gallery.comments (
    id serial primary key,
    content text not null,
    created_at timestamp not null default now(),
    user_id bigint not null,
    image_id bigint not null,
    foreign key (user_id) references user_schema.users(id) on delete cascade,
    foreign key (image_id) references gallery.images(id) on delete cascade
);

create index idx_comments_image_id on gallery.comments(image_id);
create index idx_comments_user_id on gallery.comments(user_id);

create table gallery.likes (
    id bigserial primary key,
    user_id bigint not null,
    image_id bigint not null,
    created_at timestamp not null default now(),
    foreign key (user_id) references user_schema.users(id) on delete cascade,
    foreign key (image_id) references gallery.images(id) on delete cascade,
    unique (user_id, image_id)
);

create index idx_likes_image_id on gallery.likes(image_id);
create index idx_likes_user_id on gallery.likes(user_id);

-- create publication pub
--     for all tables
--     with (publish = 'insert', publish = 'update', publish = 'delete');
--
-- create subscription sub
--     connection 'host=0.0.0.0 port=5432 user=postgres dbname=spring_microservice_course_db'
--     publication pub;
